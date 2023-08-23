package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLY_TO_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ADDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_REMOVED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
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
  private val outboundEventsService: OutboundEventsService,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentModel {
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment
    checkCaseloadAccess(appointment.prisonCode)

    val startTime = System.currentTimeMillis()
    val now = LocalDateTime.now()

    if (appointmentOccurrence.isCancelled()) {
      throw IllegalArgumentException("Cannot update a cancelled appointment occurrence")
    }

    if (LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) < now) {
      throw IllegalArgumentException("Cannot update a past appointment occurrence")
    }

    val occurrencesToUpdate = determineOccurrencesToApplyTo(appointmentOccurrence, request.applyTo, now)

    val updatedIds = mutableListOf<Long>()
    val telemetryPropertiesMap = createEditAppointmentTelemetryPropertiesMap()
    val telemetryMetricsMap = createEditAppointmentTelemetryMetricsMap()

    applyCategoryCodeUpdate(request, appointment, now, principal.name, updatedIds, telemetryPropertiesMap)
    applyStartDateUpdate(request, appointment, occurrencesToUpdate, now, principal.name, updatedIds, telemetryPropertiesMap)
    applyInternalLocationUpdate(request, appointment, occurrencesToUpdate, now, principal.name, updatedIds, telemetryPropertiesMap)
    applyStartEndTimeUpdate(request, occurrencesToUpdate, now, principal.name, updatedIds, telemetryPropertiesMap)
    applyCommentUpdate(request, occurrencesToUpdate, now, principal.name, updatedIds, telemetryPropertiesMap)
    applyAllocationUpdate(request, appointment, occurrencesToUpdate, now, principal.name, updatedIds, telemetryMetricsMap)

    val updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    updatedIds.sortedBy { it }.forEach {
      runCatching {
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it)
      }.onFailure {
        log.error(
          "Failed to send appointment instance updated event for appointment instance id $it",
          it,
        )
      }
    }

    telemetryMetricsMap[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] = (updatedIds.size + (request.removePrisonerNumbers?.size ?: 0) + (request.addPrisonerNumbers?.size ?: 0)).toDouble()
    telemetryMetricsMap[APPOINTMENT_COUNT_METRIC_KEY] = occurrencesToUpdate.size.toDouble()
    logAppointmentEditedMetric(principal, appointmentOccurrenceId, request, updatedAppointment, telemetryPropertiesMap, telemetryMetricsMap, startTime)
    return updatedAppointment.toModel()
  }

  fun cancelAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceCancelRequest, principal: Principal): AppointmentModel {
    val startTime = System.currentTimeMillis()
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    checkCaseloadAccess(appointmentOccurrence.appointment.prisonCode)

    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    val now = LocalDateTime.now()
    if (LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) < now) {
      throw IllegalArgumentException("Cannot cancel a past appointment occurrence")
    }

    val occurrencesToUpdate = determineOccurrencesToApplyTo(appointmentOccurrence, request.applyTo, now)

    occurrencesToUpdate.forEach {
      it.cancellationReason = cancellationReason
      it.cancelled = now
      it.cancelledBy = principal.name
      it.deleted = cancellationReason.isDelete
    }

    val updatedAppointment = appointmentRepository.saveAndFlush(appointmentOccurrence.appointment)

    occurrencesToUpdate.filter { it.isDeleted() }
      .flatMap { it.allocations().map { alloc -> alloc.appointmentOccurrenceAllocationId } }
      .forEach { publishDeletion(it) }
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
      .forEach { publishCancellation(it) }
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

  private fun applyCategoryCodeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
    telemetryPropertiesMap: MutableMap<String, String>,
  ) {
    request.categoryCode?.apply {
      failIfCategoryNotFound(this)

      // Category updates are applied at the appointment level
      appointment.categoryCode = this

      // Mark appointment and occurrences as updated and add associated ids for event publishing
      appointment.updated = updated
      appointment.updatedBy = updatedBy
      appointment.occurrences()
        .forEach { it.markAsUpdated(updated, updatedBy, updatedIds) }
        .also { telemetryPropertiesMap[CATEGORY_CHANGED_PROPERTY_KEY] = true.toString() }
    }
  }

  private fun applyInternalLocationUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
    telemetryPropertiesMap: MutableMap<String, String>,
  ) {
    occurrencesToUpdate.forEach {
      if (request.inCell == true) {
        it.internalLocationId = null
        it.inCell = true
        it.markAsUpdated(updated, updatedBy, updatedIds)
      } else {
        request.internalLocationId?.apply {
          failIfLocationNotFound(this, appointment.prisonCode)
          it.internalLocationId = this
          it.inCell = false
          it.markAsUpdated(updated, updatedBy, updatedIds)
          telemetryPropertiesMap[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY] = true.toString()
        }
      }
    }
  }

  private fun applyStartDateUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
    telemetryPropertiesMap: MutableMap<String, String>,

  ) {
    // Changing the start date of a repeat appointment changes the start date of all affected appointments based on the original schedule using the new start date
    request.startDate?.apply {
      val scheduleIterator = appointment.scheduleIterator().apply { startDate = request.startDate }
      occurrencesToUpdate.sortedBy { it.sequenceNumber }.forEach {
        it.startDate = scheduleIterator.next()
        it.markAsUpdated(updated, updatedBy, updatedIds)
      }.also { telemetryPropertiesMap[START_DATE_CHANGED_PROPERTY_KEY] = true.toString() }
    }
  }

  private fun applyStartEndTimeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
    telemetryPropertiesMap: MutableMap<String, String>,
  ) {
    occurrencesToUpdate.forEach {
      request.startTime?.apply {
        it.startTime = this
        it.markAsUpdated(updated, updatedBy, updatedIds)
        telemetryPropertiesMap[START_TIME_CHANGED_PROPERTY_KEY] = true.toString()
      }

      request.endTime?.apply {
        it.endTime = this
        it.markAsUpdated(updated, updatedBy, updatedIds)
        telemetryPropertiesMap[END_TIME_CHANGED_PROPERTY_KEY] = true.toString()
      }
    }
  }

  private fun applyCommentUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
    telemetryPropertiesMap: MutableMap<String, String>,
  ) {
    occurrencesToUpdate.forEach {
      request.comment?.apply {
        it.comment = this
        it.markAsUpdated(updated, updatedBy, updatedIds)
        telemetryPropertiesMap[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY] = true.toString()
      }
    }
  }

  private fun applyAllocationUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
    telemetryMetricsMap: MutableMap<String, Double>,
  ) {
    occurrencesToUpdate.forEach { occurrenceToUpdate ->

      request.removePrisonerNumbers?.forEach { prisonerToRemove ->

        if (appointment.appointmentType == AppointmentType.INDIVIDUAL && request.removePrisonerNumbers.isNotEmpty()) {
          throw IllegalArgumentException("Cannot remove prisoners from an individual appointment occurrence")
        }
        occurrenceToUpdate.allocations()
          .filter { it.prisonerNumber == prisonerToRemove }
          .forEach {
            occurrenceToUpdate.removeAllocation(it)
            // Remove id from updated list as the allocation has now been removed
            updatedIds.remove(it.appointmentOccurrenceAllocationId)
          }
        occurrenceToUpdate.updated = updated
        occurrenceToUpdate.updatedBy = updatedBy
      }

      request.addPrisonerNumbers?.apply {
        if (appointment.appointmentType == AppointmentType.INDIVIDUAL && request.addPrisonerNumbers.isNotEmpty()) {
          throw IllegalArgumentException("Cannot add prisoners to an individual appointment occurrence")
        }

        val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(this).block()!!
          .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
          .associateBy { prisoner -> prisoner.prisonerNumber }

        failIfMissingPrisoners(this, prisonerMap)

        val prisonerAllocationMap = occurrenceToUpdate.allocations().associateBy { allocation -> allocation.prisonerNumber }
        val newPrisoners = prisonerMap.filter { !prisonerAllocationMap.containsKey(it.key) }.values

        newPrisoners.forEach { prisoner ->
          occurrenceToUpdate.addAllocation(
            AppointmentOccurrenceAllocation(
              appointmentOccurrence = occurrenceToUpdate,
              prisonerNumber = prisoner.prisonerNumber,
              bookingId = prisoner.bookingId!!.toLong(),
            ),
          )
        }

        occurrenceToUpdate.updated = updated
        occurrenceToUpdate.updatedBy = updatedBy
      }
    }
  }

  private fun AppointmentOccurrence.markAsUpdated(
    updated: LocalDateTime,
    updatedBy: String,
    updatedIds: MutableList<Long>,
  ) {
    this.updated = updated
    this.updatedBy = updatedBy
    this.allocations().forEach {
      if (!updatedIds.contains(it.appointmentOccurrenceAllocationId)) {
        updatedIds.add(it.appointmentOccurrenceAllocationId)
      }
    }
  }

  private fun failIfCategoryNotFound(categoryCode: String) {
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code $categoryCode not found or is not active")
  }

  private fun failIfLocationNotFound(internalLocationId: Long, prisonCode: String) {
    locationService.getLocationsForAppointmentsMap(prisonCode)[internalLocationId]
      ?: throw IllegalArgumentException("Appointment location with id $internalLocationId not found in prison '$prisonCode'")
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }

  private fun determineOccurrencesToApplyTo(appointmentOccurrence: AppointmentOccurrence, applyTo: ApplyTo, currentTime: LocalDateTime) =
    when (applyTo) {
      ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES -> listOf(appointmentOccurrence).union(
        appointmentOccurrence.appointment.occurrences().filter { LocalDateTime.of(it.startDate, it.startTime) > LocalDateTime.of(appointmentOccurrence.startDate, appointmentOccurrence.startTime) },
      )
      ApplyTo.ALL_FUTURE_OCCURRENCES -> appointmentOccurrence.appointment.occurrences().filter { LocalDateTime.of(it.startDate, it.startTime) > currentTime }
      else -> listOf(appointmentOccurrence)
    }.filter { !it.isCancelled() }

  private fun publishCancellation(appointmentOccurrenceAllocationId: Long) = runCatching {
    outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, appointmentOccurrenceAllocationId)
  }.onFailure {
    log.error(
      "Failed to send appointment instance cancelled event for appointment instance id $it",
      it,
    )
  }

  private fun publishDeletion(appointmentOccurrenceAllocationId: Long) = runCatching {
    outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, appointmentOccurrenceAllocationId)
  }.onFailure {
    log.error(
      "Failed to send appointment instance deleted event for appointment instance id $it",
      it,
    )
  }

  private fun logAppointmentEditedMetric(
    principal: Principal,
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    telemetryPropertiesMap: MutableMap<String, String>,
    telemetryMetricsMap: MutableMap<String, Double>,
    startTimeInMs: Long,
  ) {
    telemetryPropertiesMap[USER_PROPERTY_KEY] = principal.name
    telemetryPropertiesMap[PRISON_CODE_PROPERTY_KEY] = appointment.prisonCode
    telemetryPropertiesMap[APPOINTMENT_SERIES_ID_PROPERTY_KEY] = appointment.appointmentId.toString()
    telemetryPropertiesMap[APPOINTMENT_ID_PROPERTY_KEY] = appointmentOccurrenceId.toString()
    telemetryPropertiesMap[APPLY_TO_PROPERTY_KEY] = request.applyTo.toString()

    telemetryMetricsMap[PRISONERS_ADDED_COUNT_METRIC_KEY] = request.addPrisonerNumbers?.size?.toDouble() ?: 0.0
    telemetryMetricsMap[PRISONERS_REMOVED_COUNT_METRIC_KEY] = request.removePrisonerNumbers?.size?.toDouble() ?: 0.0
    telemetryMetricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_EDITED.name, telemetryPropertiesMap, telemetryMetricsMap)
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

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_CANCELLED.name, propertiesMap, metricsMap)
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

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_DELETED.name, propertiesMap, metricsMap)
  }

  private fun createEditAppointmentTelemetryPropertiesMap() =
    mutableMapOf(
      USER_PROPERTY_KEY to "",
      PRISON_CODE_PROPERTY_KEY to "",
      APPOINTMENT_SERIES_ID_PROPERTY_KEY to "",
      APPOINTMENT_ID_PROPERTY_KEY to "",
      CATEGORY_CHANGED_PROPERTY_KEY to false.toString(),
      INTERNAL_LOCATION_CHANGED_PROPERTY_KEY to false.toString(),
      START_DATE_CHANGED_PROPERTY_KEY to false.toString(),
      START_TIME_CHANGED_PROPERTY_KEY to false.toString(),
      END_TIME_CHANGED_PROPERTY_KEY to false.toString(),
      EXTRA_INFORMATION_CHANGED_PROPERTY_KEY to false.toString(),
      APPLY_TO_PROPERTY_KEY to "",
    )

  private fun createEditAppointmentTelemetryMetricsMap() =
    mutableMapOf(
      PRISONERS_REMOVED_COUNT_METRIC_KEY to 0.0,
      PRISONERS_ADDED_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 0.0,
      EVENT_TIME_MS_METRIC_KEY to 0.0,
    )
}
