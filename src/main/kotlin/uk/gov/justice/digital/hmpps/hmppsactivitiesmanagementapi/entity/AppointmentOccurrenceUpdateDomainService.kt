package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
@Transactional
class AppointmentOccurrenceUpdateDomainService(
  private val appointmentRepository: AppointmentRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun updateAppointmentOccurrenceIds(
    appointmentId: Long,
    appointmentOccurrenceId: Long,
    occurrenceIdsToUpdate: List<Long>,
    request: AppointmentOccurrenceUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    startTimeInMs: Long,
  ): AppointmentModel {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val occurrencesToUpdate = appointment.occurrences().filter { occurrenceIdsToUpdate.contains(it.appointmentOccurrenceId) }
    return updateAppointmentOccurrences(appointment, appointmentOccurrenceId, occurrencesToUpdate, request, prisonerMap, updated, updatedBy, startTimeInMs, true)
  }

  fun updateAppointmentOccurrences(
    appointment: Appointment,
    appointmentOccurrenceId: Long,
    occurrencesToUpdate: List<AppointmentOccurrence>,
    request: AppointmentOccurrenceUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    startTimeInMs: Long,
    trackEvent: Boolean,
  ): AppointmentModel {
    val telemetryPropertiesMap = request.toTelemetryPropertiesMap(updatedBy, appointment.prisonCode, appointment.appointmentId, appointmentOccurrenceId)
    val telemetryMetricsMap = request.toTelemetryMetricsMap(occurrencesToUpdate.size, getUpdatedInstanceCount(request, appointment, occurrencesToUpdate))

    applyCategoryCodeUpdate(request, appointment, updated, updatedBy)
    applyStartDateUpdate(request, appointment, occurrencesToUpdate)
    applyInternalLocationUpdate(request, occurrencesToUpdate)
    applyStartEndTimeUpdate(request, occurrencesToUpdate)
    applyCommentUpdate(request, occurrencesToUpdate)
    applyRemovePrisonersUpdate(request, occurrencesToUpdate)

    if (request.isPropertyUpdate()) {
      occurrencesToUpdate.forEach {
        it.updated = updated
        it.updatedBy = updatedBy
      }
    }

    if (!request.addPrisonerNumbers.isNullOrEmpty()) {
      // Adding prisoners creates new allocations on the occurrence and publishes create instance events.
      // This action must be performed after other updates have been saved and flushed to prevent update events being published as well as the create events
      appointmentRepository.saveAndFlush(appointment)
      applyAddPrisonersUpdate(request, occurrencesToUpdate, prisonerMap)
    }

    val updatedAppointment = appointmentRepository.saveAndFlush(appointment)

    if (trackEvent) {
      telemetryMetricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()
      telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_EDITED.value, telemetryPropertiesMap, telemetryMetricsMap)
    }

    return updatedAppointment.toModel()
  }

  fun getUpdatedInstanceCount(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ): Int {
    var instanceCount =
      if (request.categoryCode != null) {
        appointment.scheduledOccurrences().flatMap { it.allocations() }.size
      } else if (request.isPropertyUpdate()) {
        occurrencesToUpdate.flatMap { it.allocations() }.size
      } else {
        0
      }

    // Removed instance count is implicitly included in above count if a property has changed as those updates will apply
    // to the occurrence, therefore
    if (request.removePrisonerNumbers?.isNotEmpty() == true && instanceCount == 0) {
      // only count allocations that will be removed i.e. where there's an allocation for the requested prison number
      instanceCount += occurrencesToUpdate.flatMap { it.allocations().filter { allocation -> request.removePrisonerNumbers.contains(allocation.prisonerNumber) } }.size
    }

    if (request.addPrisonerNumbers?.isNotEmpty() == true) {
      // only count allocations that will be added i.e. where there isn't already an allocation for the requested prison number
      instanceCount += occurrencesToUpdate.sumOf { occurrence ->
        request.addPrisonerNumbers.filter {
          !occurrence.allocations().map { allocation -> allocation.prisonerNumber }.contains(it)
        }.size
      }
    }

    return instanceCount
  }

  private fun applyCategoryCodeUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    appointment: Appointment,
    updated: LocalDateTime,
    updatedBy: String,
  ) {
    request.categoryCode?.apply {
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
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    occurrencesToUpdate.forEach {
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
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    occurrencesToUpdate.forEach { occurrenceToUpdate ->
      request.removePrisonerNumbers?.forEach { prisonerToRemove ->
        occurrenceToUpdate.allocations()
          .filter { it.prisonerNumber == prisonerToRemove }
          .forEach {
            occurrenceToUpdate.removeAllocation(it)
          }
      }
    }
  }

  private fun applyAddPrisonersUpdate(
    request: AppointmentOccurrenceUpdateRequest,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
    prisonerMap: Map<String, Prisoner>,
  ) {
    occurrencesToUpdate.forEach { occurrenceToUpdate ->
      request.addPrisonerNumbers?.apply {
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
      }
    }
  }
}
