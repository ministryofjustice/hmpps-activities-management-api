package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentOccurrencesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
@Transactional
class AppointmentOccurrenceService(
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val appointmentOccurrenceUpdateDomainService: AppointmentOccurrenceUpdateDomainService,
  private val appointmentOccurrenceCancelDomainService: AppointmentOccurrenceCancelDomainService,
  private val updateAppointmentOccurrencesJob: UpdateAppointmentOccurrencesJob,
  private val cancelAppointmentOccurrencesJob: CancelAppointmentOccurrencesJob,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
) {
  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointmentSeries = appointmentOccurrence.appointmentSeries
    val occurrencesToUpdate = appointmentSeries.applyToOccurrences(appointmentOccurrence, request.applyTo, "update")
    checkCaseloadAccess(appointmentSeries.prisonCode)

    if (request.categoryCode != null) {
      referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)
        .also {
          require(it.containsKey(request.categoryCode)) {
            "Appointment Category with code ${request.categoryCode} not found or is not active"
          }
        }
    }

    if (request.internalLocationId != null) {
      locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode)
        .also {
          require(it.containsKey(request.internalLocationId)) {
            "Appointment location with id ${request.internalLocationId} not found in prison '${appointmentSeries.prisonCode}'"
          }
        }
    }

    require(request.removePrisonerNumbers.isNullOrEmpty() || appointmentSeries.appointmentType != AppointmentType.INDIVIDUAL) {
      "Cannot remove prisoners from an individual appointment occurrence"
    }

    val prisonerMap = if (request.addPrisonerNumbers.isNullOrEmpty()) {
      emptyMap()
    } else {
      require(appointmentSeries.appointmentType != AppointmentType.INDIVIDUAL) {
        "Cannot add prisoners to an individual appointment occurrence"
      }
      prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers).block()!!
        .filter { prisoner -> prisoner.prisonId == appointmentSeries.prisonCode }
        .associateBy { prisoner -> prisoner.prisonerNumber }
        .also {
          val missingPrisonerNumbers = request.addPrisonerNumbers.filter { number -> !it.containsKey(number) }
          require(missingPrisonerNumbers.isEmpty()) {
            "Prisoner(s) with prisoner number(s) '${missingPrisonerNumbers.joinToString("', '")}' not found, were inactive or are residents of a different prison."
          }
        }
    }

    val updateOccurrencesCount = occurrencesToUpdate.size
    val updateInstancesCount = appointmentOccurrenceUpdateDomainService.getUpdateInstancesCount(request, appointmentSeries, occurrencesToUpdate)
    // Determine if this is an update request that will affect more than one occurrence and a very large number of appointment instances. If it is, only update the first occurrence
    val updateFirstOccurrenceOnly = updateOccurrencesCount > 1 && updateInstancesCount > maxSyncAppointmentInstanceActions

    val updatedAppointment = appointmentOccurrenceUpdateDomainService.updateAppointmentOccurrences(
      appointmentSeries,
      appointmentOccurrenceId,
      if (updateFirstOccurrenceOnly) setOf(appointmentOccurrence) else occurrencesToUpdate.toSet(),
      request,
      prisonerMap,
      now,
      principal.name,
      updateOccurrencesCount,
      updateInstancesCount,
      startTimeInMs,
      !updateFirstOccurrenceOnly,
      true,
    )

    if (updateFirstOccurrenceOnly) {
      // The remaining occurrences will be updated asynchronously by this job
      updateAppointmentOccurrencesJob.execute(
        appointmentSeries.appointmentSeriesId,
        appointmentOccurrenceId,
        occurrencesToUpdate.filterNot { it.appointmentOccurrenceId == appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet(),
        request,
        prisonerMap,
        now,
        principal.name,
        updateOccurrencesCount,
        updateInstancesCount,
        startTimeInMs,
      )
    }

    return updatedAppointment
  }

  fun cancelAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceCancelRequest, principal: Principal): AppointmentModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointmentSeries = appointmentOccurrence.appointmentSeries
    val occurrencesToCancel = appointmentSeries.applyToOccurrences(appointmentOccurrence, request.applyTo, "cancel")
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val cancelOccurrencesCount = occurrencesToCancel.size
    val cancelInstancesCount = appointmentOccurrenceCancelDomainService.getCancelInstancesCount(occurrencesToCancel)
    // Determine if this is a cancel request that will affect more than one occurrence and a very large number of appointment instances. If it is, only cancel the first occurrence
    val cancelFirstOccurrenceOnly = cancelOccurrencesCount > 1 && cancelInstancesCount > maxSyncAppointmentInstanceActions

    val cancelledAppointment = appointmentOccurrenceCancelDomainService.cancelAppointmentOccurrences(
      appointmentSeries,
      appointmentOccurrenceId,
      if (cancelFirstOccurrenceOnly) setOf(appointmentOccurrence) else occurrencesToCancel.toSet(),
      request,
      now,
      principal.name,
      cancelOccurrencesCount,
      cancelInstancesCount,
      startTimeInMs,
      !cancelFirstOccurrenceOnly,
      true,
    )

    if (cancelFirstOccurrenceOnly) {
      // The remaining occurrences will be updated asynchronously by this job
      cancelAppointmentOccurrencesJob.execute(
        appointmentSeries.appointmentSeriesId,
        appointmentOccurrenceId,
        occurrencesToCancel.filterNot { it.appointmentOccurrenceId == appointmentOccurrenceId }.map { it.appointmentOccurrenceId }.toSet(),
        request,
        now,
        principal.name,
        cancelOccurrencesCount,
        cancelInstancesCount,
        startTimeInMs,
      )
    }

    return cancelledAppointment
  }
}
