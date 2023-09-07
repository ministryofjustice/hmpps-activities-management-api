package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentSeriesModel

@Service
@Transactional
class AppointmentService(
  private val appointmentRepository: AppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val appointmentUpdateDomainService: AppointmentUpdateDomainService,
  private val appointmentCancelDomainService: AppointmentCancelDomainService,
  private val updateAppointmentsJob: UpdateAppointmentsJob,
  private val cancelAppointmentsJob: CancelAppointmentsJob,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
) {
  fun updateAppointment(appointmentId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentSeriesModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val appointmentSeries = appointment.appointmentSeries
    val appointmentsToUpdate = appointmentSeries.applyToAppointments(appointment, request.applyTo, "update")
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
      "Cannot remove prisoners from an individual appointment"
    }

    val prisonerMap = if (request.addPrisonerNumbers.isNullOrEmpty()) {
      emptyMap()
    } else {
      require(appointmentSeries.appointmentType != AppointmentType.INDIVIDUAL) {
        "Cannot add prisoners to an individual appointment"
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

    val updateAppointmentsCount = appointmentsToUpdate.size
    val updateInstancesCount = appointmentUpdateDomainService.getUpdateInstancesCount(request, appointmentSeries, appointmentsToUpdate)
    // Determine if this is an update request that will affect more than one appointment and a very large number of appointment instances. If it is, only update the first appointment
    val updateFirstAppointmentOnly = updateAppointmentsCount > 1 && updateInstancesCount > maxSyncAppointmentInstanceActions

    val updatedAppointment = appointmentUpdateDomainService.updateAppointments(
      appointmentSeries,
      appointmentId,
      if (updateFirstAppointmentOnly) setOf(appointment) else appointmentsToUpdate.toSet(),
      request,
      prisonerMap,
      now,
      principal.name,
      updateAppointmentsCount,
      updateInstancesCount,
      startTimeInMs,
      !updateFirstAppointmentOnly,
      true,
    )

    if (updateFirstAppointmentOnly) {
      // The remaining appointments will be updated asynchronously by this job
      updateAppointmentsJob.execute(
        appointmentSeries.appointmentSeriesId,
        appointmentId,
        appointmentsToUpdate.filterNot { it.appointmentId == appointmentId }.map { it.appointmentId }.toSet(),
        request,
        prisonerMap,
        now,
        principal.name,
        updateAppointmentsCount,
        updateInstancesCount,
        startTimeInMs,
      )
    }

    return updatedAppointment
  }

  fun cancelAppointment(appointmentId: Long, request: AppointmentOccurrenceCancelRequest, principal: Principal): AppointmentSeriesModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val appointmentSeries = appointment.appointmentSeries
    val appointmentsToCancel = appointmentSeries.applyToAppointments(appointment, request.applyTo, "cancel")
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val cancelAppointmentsCount = appointmentsToCancel.size
    val cancelInstancesCount = appointmentCancelDomainService.getCancelInstancesCount(appointmentsToCancel)
    // Determine if this is a cancel request that will affect more than one appointment and a very large number of appointment instances. If it is, only cancel the first appointment
    val cancelFirstAppointmentOnly = cancelAppointmentsCount > 1 && cancelInstancesCount > maxSyncAppointmentInstanceActions

    val cancelledAppointment = appointmentCancelDomainService.cancelAppointments(
      appointmentSeries,
      appointmentId,
      if (cancelFirstAppointmentOnly) setOf(appointment) else appointmentsToCancel.toSet(),
      request,
      now,
      principal.name,
      cancelAppointmentsCount,
      cancelInstancesCount,
      startTimeInMs,
      !cancelFirstAppointmentOnly,
      true,
    )

    if (cancelFirstAppointmentOnly) {
      // The remaining appointments will be updated asynchronously by this job
      cancelAppointmentsJob.execute(
        appointmentSeries.appointmentSeriesId,
        appointmentId,
        appointmentsToCancel.filterNot { it.appointmentId == appointmentId }.map { it.appointmentId }.toSet(),
        request,
        now,
        principal.name,
        cancelAppointmentsCount,
        cancelInstancesCount,
        startTimeInMs,
      )
    }

    return cancelledAppointment
  }
}
