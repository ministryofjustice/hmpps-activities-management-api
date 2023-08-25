package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLY_TO_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
@Transactional
class AppointmentOccurrenceService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val telemetryClient: TelemetryClient,
) {
  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment
    checkCaseloadAccess(appointment.prisonCode)

    require(!appointmentOccurrence.isCancelled()) {
      "Cannot update a cancelled appointment occurrence"
    }

    require(!appointmentOccurrence.isExpired()) {
      "Cannot update a past appointment occurrence"
    }

    val categoryMap = if (request.categoryCode?.isEmpty() == true) emptyMap() else referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)
    val locationMap = if (request.internalLocationId == null) emptyMap() else locationService.getLocationsForAppointmentsMap(appointment.prisonCode)
    val prisonerMap = if (request.addPrisonerNumbers.isNullOrEmpty()) {
      emptyMap()
    } else {
      require(appointment.appointmentType != AppointmentType.INDIVIDUAL) {
        "Cannot add prisoners to an individual appointment occurrence"
      }
      prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers).block()!!
        .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
        .associateBy { prisoner -> prisoner.prisonerNumber }
        .also {
          val missingPrisonerNumbers = request.addPrisonerNumbers.filter { number -> !it.containsKey(number) }
          require(missingPrisonerNumbers.isEmpty()) {
            "Prisoner(s) with prisoner number(s) '${missingPrisonerNumbers.joinToString("', '")}' not found, were inactive or are residents of a different prison."
          }
        }
    }

    val occurrencesToUpdate = determineOccurrencesToApplyTo(appointment, appointmentOccurrence, request.applyTo)
    var instanceCount = getUpdatedInstanceCount(request, appointment, occurrencesToUpdate)

    val telemetryPropertiesMap = request.toTelemetryPropertiesMap(principal.name, appointment.prisonCode, appointment.appointmentId, appointmentOccurrence.appointmentOccurrenceId)
    val telemetryMetricsMap = request.toTelemetryMetricsMap(occurrencesToUpdate.size)

    applyCategoryCodeUpdate(request, appointment, categoryMap, now, principal.name)
    applyStartDateUpdate(request, appointment, occurrencesToUpdate)
    applyInternalLocationUpdate(request, appointment, occurrencesToUpdate, locationMap)
    applyStartEndTimeUpdate(request, occurrencesToUpdate)
    applyCommentUpdate(request, occurrencesToUpdate)
    instanceCount += applyRemovePrisonersUpdate(request, appointment, occurrencesToUpdate)

    if (request.isPropertyUpdate()) {
      occurrencesToUpdate.forEach {
        it.updated = now
        it.updatedBy = principal.name
      }
    }

    var updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    // Adding prisoners creates new allocations on the occurrence and publishes create instance events.
    // This action must be performed after other updates have been saved and flushed to prevent update events being published as well as the create events
    applyAddPrisonersUpdate(request, occurrencesToUpdate, prisonerMap).also {
      if (it > 0) {
        updatedAppointment = appointmentRepository.saveAndFlush(appointment)
        instanceCount += it
      }
    }

    telemetryMetricsMap[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] = instanceCount.toDouble()
    telemetryMetricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()
    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_EDITED.value, telemetryPropertiesMap, telemetryMetricsMap)

    return updatedAppointment.toModel()
  }

  fun cancelAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceCancelRequest, principal: Principal): AppointmentModel {
    val startTime = System.currentTimeMillis()
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment
    checkCaseloadAccess(appointment.prisonCode)

    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    val now = LocalDateTime.now()
    if (LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) < now) {
      throw IllegalArgumentException("Cannot cancel a past appointment occurrence")
    }

    val occurrencesToUpdate = determineOccurrencesToApplyTo(appointment, appointmentOccurrence, request.applyTo)

    occurrencesToUpdate.forEach {
      it.cancellationReason = cancellationReason
      it.cancelled = now
      it.cancelledBy = principal.name
      it.deleted = cancellationReason.isDelete
    }

    val updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    occurrencesToUpdate.filter { it.isDeleted() }
      .flatMap { it.allocations().map { alloc -> alloc.appointmentOccurrenceAllocationId } }
      .also {
        logAppointmentDeletedMetric(
          principal,
          appointmentOccurrenceId,
          request,
          updatedAppointment,
          occurrencesToUpdate.size,
          occurrencesToUpdate.flatMap { it.allocations() }.size,
          startTime,
        )
      }

    occurrencesToUpdate.filter { it.isCancelled() }
      .flatMap { it.allocations().map { alloc -> alloc.appointmentOccurrenceAllocationId } }
      .also {
        logAppointmentCancelledMetric(
          principal,
          appointmentOccurrenceId,
          request,
          updatedAppointment,
          occurrencesToUpdate.size,
          occurrencesToUpdate.flatMap { it.allocations() }.size,
          startTime,
        )
      }

    return updatedAppointment.toModel()
  }

  private fun getUpdatedInstanceCount(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) =
    if (request.categoryCode != null) {
      appointment.scheduledOccurrences().flatMap { it.allocations() }.size
    } else if (request.isPropertyUpdate()) {
      occurrencesToUpdate.flatMap { it.allocations() }.size
    } else {
      0
    }

  private fun applyCategoryCodeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    categoryMap: Map<String, ReferenceCode>,
    updated: LocalDateTime,
    updatedBy: String,
  ) {
    request.categoryCode?.apply {
      require(categoryMap.containsKey(this)) {
        "Appointment Category with code $this not found or is not active"
      }

      // Category updates are applied at the appointment level
      appointment.categoryCode = this

      // Mark appointment and occurrences as updated
      appointment.updated = updated
      appointment.updatedBy = updatedBy

      appointment.scheduledOccurrences().forEach {
        it.updated = updated
        it.updatedBy = updatedBy
      }
    }
  }

  private fun applyInternalLocationUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    locationMap: Map<Long, Location>,
  ) {
    occurrencesToUpdate.forEach {
      if (request.inCell == true) {
        it.internalLocationId = null
        it.inCell = true
      } else {
        request.internalLocationId?.apply {
          require(locationMap.containsKey(this)) {
            "Appointment location with id $this not found in prison '${appointment.prisonCode}'"
          }
          it.internalLocationId = this
          it.inCell = false
        }
      }
    }
  }

  private fun applyStartDateUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    // Changing the start date of a repeat appointment changes the start date of all affected appointments based on the original schedule using the new start date
    request.startDate?.apply {
      val scheduleIterator = appointment.scheduleIterator().apply { startDate = request.startDate }
      occurrencesToUpdate.sortedBy { it.sequenceNumber }.forEach {
        it.startDate = scheduleIterator.next()
      }
    }
  }

  private fun applyStartEndTimeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    occurrencesToUpdate.forEach {
      request.startTime?.apply {
        it.startTime = this
      }

      request.endTime?.apply {
        it.endTime = this
      }
    }
  }

  private fun applyCommentUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    occurrencesToUpdate.forEach {
      request.comment?.apply {
        it.comment = this
      }
    }
  }

  private fun applyRemovePrisonersUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ): Int {
    var removedInstanceCount = 0
    occurrencesToUpdate.forEach { occurrenceToUpdate ->
      request.removePrisonerNumbers?.forEach { prisonerToRemove ->
        if (appointment.appointmentType == AppointmentType.INDIVIDUAL && request.removePrisonerNumbers.isNotEmpty()) {
          throw IllegalArgumentException("Cannot remove prisoners from an individual appointment occurrence")
        }
        occurrenceToUpdate.allocations()
          .filter { it.prisonerNumber == prisonerToRemove }
          .forEach {
            occurrenceToUpdate.removeAllocation(it)
            removedInstanceCount++
          }
      }
    }
    return removedInstanceCount
  }

  private fun applyAddPrisonersUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    prisonerMap: Map<String, Prisoner>,
  ): Int {
    var newInstanceCount = 0
    occurrencesToUpdate.forEach { occurrenceToUpdate ->
      request.addPrisonerNumbers?.apply {
        val prisonerAllocationMap = occurrenceToUpdate.allocations().associateBy { allocation -> allocation.prisonerNumber }
        val newPrisoners = prisonerMap.filter { !prisonerAllocationMap.containsKey(it.key) }.values
          .also { newInstanceCount += it.size }

        newPrisoners.forEach { prisoner ->
          occurrenceToUpdate.addAllocation(
            AppointmentOccurrenceAllocation(
              appointmentOccurrence = occurrenceToUpdate,
              prisonerNumber = prisoner.prisonerNumber,
              bookingId = prisoner.bookingId!!.toLong(),
            ),
          )
        }
      }
    }
    return newInstanceCount
  }

  private fun determineOccurrencesToApplyTo(appointment: Appointment, appointmentOccurrence: AppointmentOccurrence, applyTo: ApplyTo) =
    when (applyTo) {
      ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointmentOccurrence).union(
        appointment.scheduledOccurrencesAfter(appointmentOccurrence.startDateTime()),
      )
      ApplyTo.ALL_FUTURE_OCCURRENCES -> appointment.scheduledOccurrences()
      else -> listOf(appointmentOccurrence)
    }

  private fun logAppointmentCancelledMetric(
    principal: Principal,
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointment: Appointment,
    occurrencesCancelled: Int,
    instancesCancelled: Int,
    startTimeInMs: Long,
  ) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointment.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointment.appointmentId.toString(),
      APPOINTMENT_ID_PROPERTY_KEY to appointmentOccurrenceId.toString(),
      APPLY_TO_PROPERTY_KEY to request.applyTo.toString(),
    )

    val metricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to occurrencesCancelled.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to instancesCancelled.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_CANCELLED.value, propertiesMap, metricsMap)
  }

  private fun logAppointmentDeletedMetric(
    principal: Principal,
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointment: Appointment,
    occurrencesDeleted: Int,
    instancesDeleted: Int,
    startTimeInMs: Long,
  ) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to appointment.prisonCode,
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointment.appointmentId.toString(),
      APPOINTMENT_ID_PROPERTY_KEY to appointmentOccurrenceId.toString(),
      APPLY_TO_PROPERTY_KEY to request.applyTo.toString(),
    )

    val metricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to occurrencesDeleted.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to instancesDeleted.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_DELETED.value, propertiesMap, metricsMap)
  }
}
