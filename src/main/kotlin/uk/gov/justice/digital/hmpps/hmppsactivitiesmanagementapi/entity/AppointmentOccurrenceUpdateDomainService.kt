package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentOccurrenceEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
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
  private val auditService: AuditService,
) {
  fun updateAppointmentOccurrenceIds(
    appointmentId: Long,
    appointmentOccurrenceId: Long,
    occurrenceIdsToUpdate: Set<Long>,
    request: AppointmentOccurrenceUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    updateOccurrencesCount: Int,
    updateInstancesCount: Int,
    startTimeInMs: Long,
  ): AppointmentModel {
    val appointmentSeries = appointmentRepository.findOrThrowNotFound(appointmentId)
    val occurrencesToUpdate = appointmentSeries.appointments().filter { occurrenceIdsToUpdate.contains(it.appointmentOccurrenceId) }.toSet()
    return updateAppointmentOccurrences(appointmentSeries, appointmentOccurrenceId, occurrencesToUpdate, request, prisonerMap, updated, updatedBy, updateOccurrencesCount, updateInstancesCount, startTimeInMs, true, false)
  }

  fun updateAppointmentOccurrences(
    appointmentSeries: AppointmentSeries,
    appointmentOccurrenceId: Long,
    occurrencesToUpdate: Set<AppointmentOccurrence>,
    request: AppointmentOccurrenceUpdateRequest,
    prisonerMap: Map<String, Prisoner>,
    updated: LocalDateTime,
    updatedBy: String,
    updateOccurrencesCount: Int,
    updateInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
    auditEvent: Boolean,
  ): AppointmentModel {
    applyCategoryCodeUpdate(request, occurrencesToUpdate)
    applyStartDateUpdate(request, appointmentSeries, occurrencesToUpdate)
    applyInternalLocationUpdate(request, occurrencesToUpdate)
    applyStartEndTimeUpdate(request, occurrencesToUpdate)
    applyCommentUpdate(request, occurrencesToUpdate)
    applyRemovePrisonersUpdate(request, occurrencesToUpdate)

    if (request.isPropertyUpdate()) {
      appointmentSeries.updatedTime = updated
      appointmentSeries.updatedBy = updatedBy
      occurrencesToUpdate.forEach {
        it.updated = updated
        it.updatedBy = updatedBy
      }
    }

    if (!request.addPrisonerNumbers.isNullOrEmpty()) {
      // Adding prisoners creates new allocations on the occurrence and publishes create instance events.
      // This action must be performed after other updates have been saved and flushed to prevent update events being published as well as the create events
      appointmentRepository.saveAndFlush(appointmentSeries)
      applyAddPrisonersUpdate(request, occurrencesToUpdate, prisonerMap)
    }

    val updatedAppointment = appointmentRepository.saveAndFlush(appointmentSeries)

    if (trackEvent) {
      val telemetryPropertiesMap = request.toTelemetryPropertiesMap(updatedBy, appointmentSeries.prisonCode, appointmentSeries.appointmentSeriesId, appointmentOccurrenceId)
      val telemetryMetricsMap = request.toTelemetryMetricsMap(updateOccurrencesCount, updateInstancesCount)
      telemetryMetricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()
      telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_EDITED.value, telemetryPropertiesMap, telemetryMetricsMap)
    }

    if (auditEvent) {
      writeAppointmentOccurrenceUpdatedAuditRecord(appointmentOccurrenceId, request, appointmentSeries, updatedAppointment)
    }

    return updatedAppointment.toModel()
  }

  fun getUpdateInstancesCount(
    request: AppointmentOccurrenceUpdateRequest,
    appointmentSeries: AppointmentSeries,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ): Int {
    var instanceCount = if (request.isPropertyUpdate()) occurrencesToUpdate.flatMap { it.allocations() }.size else 0

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
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    occurrencesToUpdate.forEach {
      request.categoryCode?.apply {
        it.categoryCode = this
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
    appointmentSeries: AppointmentSeries,
    occurrencesToUpdate: Collection<AppointmentOccurrence>,
  ) {
    // Changing the start date of a repeat appointment changes the start date of all affected appointments based on the original schedule using the new start date
    request.startDate?.apply {
      val scheduleIterator = appointmentSeries.scheduleIterator().apply { startDate = request.startDate }
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
        val existingPrisonNumbers = occurrenceToUpdate.allocations().map { allocation -> allocation.prisonerNumber }
        val newPrisonNumbers = this.filterNot { existingPrisonNumbers.contains(it) }
        newPrisonNumbers.forEach {
          occurrenceToUpdate.addAllocation(
            AppointmentOccurrenceAllocation(
              appointmentOccurrence = occurrenceToUpdate,
              prisonerNumber = it,
              bookingId = prisonerMap[it]!!.bookingId!!.toLong(),
            ),
          )
        }
      }
    }
  }

  private fun writeAppointmentOccurrenceUpdatedAuditRecord(
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceUpdateRequest,
    originalAppointmentSeries: AppointmentSeries,
    updatedAppointmentSeries: AppointmentSeries,
  ) {
    auditService.logEvent(
      AppointmentOccurrenceEditedEvent(
        appointmentSeriesId = originalAppointmentSeries.appointmentSeriesId,
        appointmentId = appointmentOccurrenceId,
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
