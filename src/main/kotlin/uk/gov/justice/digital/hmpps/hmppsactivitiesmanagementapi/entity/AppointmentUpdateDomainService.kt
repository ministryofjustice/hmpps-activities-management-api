package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PERMANENT_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

@Service
@Transactional
class AppointmentUpdateDomainService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentRepository: AppointmentRepository,
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  fun updateAppointmentIds(
    appointmentSeriesId: Long,
    appointmentId: Long,
    appointmentIdsToUpdate: Set<Long>,
    request: AppointmentUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    updateAppointmentsCount: Int,
    updateInstancesCount: Int,
    startTimeInMs: Long,
  ): AppointmentSeriesModel {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    val appointmentsToUpdate = appointmentSeries.appointments().filter { appointmentIdsToUpdate.contains(it.appointmentId) }.toSet()
    return updateAppointments(appointmentSeries, appointmentId, appointmentsToUpdate, request, prisonerMap, updated, updatedBy, updateAppointmentsCount, updateInstancesCount, startTimeInMs, true, false)
  }

  fun updateAppointments(
    appointmentSeries: AppointmentSeries,
    appointmentId: Long,
    appointmentsToUpdate: Set<Appointment>,
    request: AppointmentUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    updateAppointmentsCount: Int,
    updateInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
    auditEvent: Boolean,
  ): AppointmentSeriesModel {
    applyPropertyUpdates(request, appointmentSeries, appointmentsToUpdate)
    val removedAttendees = applyRemovePrisonersUpdate(request, appointmentsToUpdate, updated, updatedBy)
    val addedAttendees = applyAddPrisonersUpdate(request, appointmentsToUpdate, prisonerMap, updated, updatedBy)

    appointmentsToUpdate.forEach {
      it.updatedTime = updated
      it.updatedBy = updatedBy
    }

    transactionHandler.newSpringTransaction {
      appointmentRepository.saveAllAndFlush(appointmentsToUpdate)
    }.also {
      val removedAttendeeIds = removedAttendees.map { it.appointmentAttendeeId }.toSet().onEach {
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, it)
      }
      val addedAttendeesIds = addedAttendees.map { it.appointmentAttendeeId }.toSet().onEach {
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, it)
      }
      if (request.isPropertyUpdate()) {
        appointmentsToUpdate
          .flatMap { it.attendees().map { attendee -> attendee.appointmentAttendeeId } }
          .filter { !removedAttendeeIds.contains(it) && !addedAttendeesIds.contains(it) }
          .forEach { outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it) }
      }
    }

    appointmentSeries.updatedTime = updated
    appointmentSeries.updatedBy = updatedBy
    val updatedAppointmentSeries = appointmentSeriesRepository.saveAndFlush(appointmentSeries)

    if (trackEvent) {
      val telemetryPropertiesMap = request.toTelemetryPropertiesMap(updatedBy, updatedAppointmentSeries.prisonCode, updatedAppointmentSeries.appointmentSeriesId, appointmentId)
      val telemetryMetricsMap = request.toTelemetryMetricsMap(updateAppointmentsCount, updateInstancesCount)
      telemetryMetricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()
      telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_EDITED.value, telemetryPropertiesMap, telemetryMetricsMap)
    }

    if (auditEvent) {
      writeAppointmentUpdatedAuditRecord(appointmentId, request, updatedAppointmentSeries, updatedAppointmentSeries)
    }

    return updatedAppointmentSeries.toModel()
  }

  fun getUpdateInstancesCount(
    request: AppointmentUpdateRequest,
    appointmentSeries: AppointmentSeries,
    appointmentsToUpdate: Collection<Appointment>,
  ): Int {
    var instanceCount = if (request.isPropertyUpdate()) appointmentsToUpdate.flatMap { it.attendees() }.size else 0

    // Removed instance count is implicitly included in above count if a property has changed as those updates will apply
    // to the appointment, therefore
    if (request.removePrisonerNumbers?.isNotEmpty() == true && instanceCount == 0) {
      // only count attendees that will be removed i.e. where there's an attendee for the requested prison number
      instanceCount += appointmentsToUpdate.flatMap { it.attendees().filter { attendee -> request.removePrisonerNumbers.contains(attendee.prisonerNumber) } }.size
    }

    if (request.addPrisonerNumbers?.isNotEmpty() == true) {
      // only count attendees that will be added i.e. where there isn't already an attendee for the requested prison number
      instanceCount += appointmentsToUpdate.sumOf { appointment ->
        request.addPrisonerNumbers.filter {
          !appointment.attendees().map { attendee -> attendee.prisonerNumber }.contains(it)
        }.size
      }
    }

    return instanceCount
  }

  private fun applyCategoryCodeUpdate(
    request: AppointmentUpdateRequest,
    appointmentsToUpdate: Collection<Appointment>,
  ) {
    appointmentsToUpdate.forEach {
      request.categoryCode?.apply {
        it.categoryCode = this
      }
    }
  }

  private fun applyInternalLocationUpdate(
    request: AppointmentUpdateRequest,
    appointmentsToUpdate: Collection<Appointment>,
  ) {
    appointmentsToUpdate.forEach {
      if (request.inCell == true) {
        it.internalLocationId = null
        it.inCell = true
      } else {
        request.internalLocationId?.apply {
          it.internalLocationId = this
          it.inCell = false
        }
      }
    }
  }

  private fun applyStartDateUpdate(
    request: AppointmentUpdateRequest,
    appointmentSeries: AppointmentSeries,
    appointmentsToUpdate: Collection<Appointment>,
  ) {
    // Changing the start date of a repeat appointment changes the start date of all affected appointments based on the original schedule using the new start date
    request.startDate?.apply {
      val scheduleIterator = appointmentSeries.scheduleIterator().apply { startDate = request.startDate }
      appointmentsToUpdate.sortedBy { it.sequenceNumber }.forEach {
        it.startDate = scheduleIterator.next()
      }
    }
  }

  private fun applyStartEndTimeUpdate(
    request: AppointmentUpdateRequest,
    appointmentsToUpdate: Collection<Appointment>,
  ) {
    appointmentsToUpdate.forEach {
      request.startTime?.apply {
        it.startTime = this
      }

      request.endTime?.apply {
        it.endTime = this
      }
    }
  }

  private fun applyExtraInformationUpdate(
    request: AppointmentUpdateRequest,
    appointmentsToUpdate: Collection<Appointment>,
  ) {
    appointmentsToUpdate.forEach {
      request.extraInformation?.apply {
        it.extraInformation = this
      }
    }
  }

  private fun applyRemovePrisonersUpdate(
    request: AppointmentUpdateRequest,
    appointmentsToUpdate: Collection<Appointment>,
    updated: LocalDateTime,
    updatedBy: String,
  ): List<AppointmentAttendee> {
    if (request.removePrisonerNumbers.isNullOrEmpty()) return emptyList()

    val removalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(PERMANENT_REMOVAL_BY_USER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID)
    return appointmentsToUpdate.flatMap { appointmentToUpdate ->
      request.removePrisonerNumbers.flatMap { prisonerToRemove ->
        appointmentToUpdate.removeAttendee(prisonerToRemove, updated, removalReason, updatedBy)
      }
    }
  }

  private fun applyPropertyUpdates(
    request: AppointmentUpdateRequest,
    appointmentSeries: AppointmentSeries,
    appointmentsToUpdate: Collection<Appointment>,
  ) {
    applyCategoryCodeUpdate(request, appointmentsToUpdate)
    applyStartDateUpdate(request, appointmentSeries, appointmentsToUpdate)
    applyInternalLocationUpdate(request, appointmentsToUpdate)
    applyStartEndTimeUpdate(request, appointmentsToUpdate)
    applyExtraInformationUpdate(request, appointmentsToUpdate)
  }

  private fun applyAddPrisonersUpdate(
    request: AppointmentUpdateRequest,
    appointmentsToUpdate: Collection<Appointment>,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
  ): List<AppointmentAttendee> {
    if (request.addPrisonerNumbers.isNullOrEmpty()) return emptyList()

    return appointmentsToUpdate.flatMap { appointmentToUpdate ->
      request.addPrisonerNumbers.mapNotNull { prisonerToAdd ->
        appointmentToUpdate.addAttendee(prisonerToAdd, prisonerMap[prisonerToAdd]!!.bookingId!!.toLong(), updated, updatedBy)
      }
    }
  }

  private fun writeAppointmentUpdatedAuditRecord(
    appointmentId: Long,
    request: AppointmentUpdateRequest,
    originalAppointmentSeries: AppointmentSeries,
    updatedAppointmentSeries: AppointmentSeries,
  ) {
    auditService.logEvent(
      AppointmentEditedEvent(
        appointmentSeriesId = originalAppointmentSeries.appointmentSeriesId,
        appointmentId = appointmentId,
        prisonCode = originalAppointmentSeries.prisonCode,
        originalCategoryCode = originalAppointmentSeries.categoryCode,
        categoryCode = updatedAppointmentSeries.categoryCode,
        originalInternalLocationId = originalAppointmentSeries.internalLocationId,
        internalLocationId = updatedAppointmentSeries.internalLocationId,
        originalStartDate = originalAppointmentSeries.startDate,
        startDate = updatedAppointmentSeries.startDate,
        originalStartTime = originalAppointmentSeries.startTime,
        startTime = updatedAppointmentSeries.startTime,
        originalEndTime = originalAppointmentSeries.endTime,
        endTime = updatedAppointmentSeries.endTime,
        applyTo = request.applyTo,
        createdAt = LocalDateTime.now(),
      ),
    )
  }
}
