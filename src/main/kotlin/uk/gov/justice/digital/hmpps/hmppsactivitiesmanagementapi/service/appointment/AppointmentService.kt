package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UncancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ScheduleReasonEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

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
  private val uncancelAppointmentsJob: UncancelAppointmentsJob,
  @Value("\${applications.max-appointment-instances}") private val maxAppointmentInstances: Int = 20000,
  @Value("\${applications.max-sync-appointment-instance-actions}") private val maxSyncAppointmentInstanceActions: Int = 500,
  @Value("\${applications.max-appointment-start-date-from-today:370}") private val maxStartDateOffsetDays: Long = 370,
) {
  @Transactional(readOnly = true)
  fun getAppointmentDetailsById(appointmentId: Long): AppointmentDetails {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.appointmentSeries.prisonCode)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(appointment.prisonerNumbers())

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationDetailsForAppointmentsMap(appointment.prisonCode)

    return appointment.toDetails(prisonerMap, referenceCodeMap, locationMap)
  }

  @Transactional(readOnly = true)
  fun getAppointmentDetailsByIds(appointmentIds: List<Long>): List<AppointmentDetails> {
    val appointments = appointmentRepository.findByIds(appointmentIds)

    appointments.forEach { checkCaseloadAccess(it.appointmentSeries.prisonCode) }

    val prisonerNumbers = appointments.map { it.prisonerNumbers() }.flatten().distinct()

    val prisonCodes = appointments.map { it.prisonCode }.distinct()

    require(prisonCodes.size == 1) { "Only one prison code is supported" }

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(prisonerNumbers)

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationDetailsForAppointmentsMap(prisonCodes.first())

    return appointments.map { it.toDetails(prisonerMap, referenceCodeMap, locationMap) }
  }

  fun updateAppointment(appointmentId: Long, request: AppointmentUpdateRequest, principal: Principal): AppointmentSeriesModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val appointmentSeries = appointment.appointmentSeries
    val appointmentsToUpdate = appointmentSeries.applyToAppointments(appointment, request.applyTo, "update", false)
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
      locationService.getLocationDetailsForAppointmentsMap(appointmentSeries.prisonCode)
        .also {
          require(it.containsKey(request.internalLocationId)) {
            "Appointment location with Nomis location id ${request.internalLocationId} not found in prison '${appointmentSeries.prisonCode}'"
          }
        }
    }

    if (request.dpsLocationId != null) {
      locationService.getLocationDetailsForAppointmentsMapByDpsLocationId(appointmentSeries.prisonCode)
        .also {
          require(it.containsKey(request.dpsLocationId)) {
            "Appointment location with DPS location id ${request.dpsLocationId} not found in prison '${appointmentSeries.prisonCode}'"
          }
        }
    }

    require(request.removePrisonerNumbers.isNullOrEmpty() || appointmentSeries.appointmentType != AppointmentType.INDIVIDUAL) {
      "Cannot remove prisoners from an individual appointment"
    }

    require(request.startDate == null || request.startDate <= LocalDate.now().plusDays(maxStartDateOffsetDays)) {
      "Start date cannot be more than $maxStartDateOffsetDays days into the future"
    }

    val prisonerMap = if (request.addPrisonerNumbers.isNullOrEmpty()) {
      emptyMap()
    } else {
      require(appointmentSeries.appointmentType != AppointmentType.INDIVIDUAL) {
        "Cannot add prisoners to an individual appointment"
      }
      prisonerSearchApiClient.findByPrisonerNumbers(request.addPrisonerNumbers)
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
    val updateInstancesCount = appointmentUpdateDomainService.getUpdateInstancesCount(request, appointmentsToUpdate)
    require(updateInstancesCount <= maxAppointmentInstances) {
      "You cannot modify more than ${maxAppointmentInstances / updateAppointmentsCount} appointment instances for this number of attendees"
    }

    // Determine if this is an update request that will affect more than one appointment and a very large number of appointment instances. If it is, only update the first appointment
    val updateFirstAppointmentOnly = updateAppointmentsCount > 1 && updateInstancesCount > maxSyncAppointmentInstanceActions

    val updatedAppointmentSeries = appointmentUpdateDomainService.updateAppointments(
      appointmentSeries.appointmentSeriesId,
      appointmentId,
      if (updateFirstAppointmentOnly) setOf(appointment.appointmentId) else appointmentsToUpdate.map { it.appointmentId }.toSet(),
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

    return updatedAppointmentSeries
  }

  fun cancelAppointment(appointmentId: Long, request: AppointmentCancelRequest, principal: Principal): AppointmentSeriesModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val appointmentSeries = appointment.appointmentSeries
    val appointmentsToCancel = appointmentSeries.applyToAppointments(appointment, request.applyTo, "cancel", false)
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val cancelAppointmentsCount = appointmentsToCancel.size
    val cancelInstancesCount = appointmentCancelDomainService.getCancelInstancesCount(appointmentsToCancel)
    // Determine if this is a cancel request that will affect more than one appointment and a very large number of appointment instances. If it is, only cancel the first appointment
    val cancelFirstAppointmentOnly = cancelAppointmentsCount > 1 && cancelInstancesCount > maxSyncAppointmentInstanceActions

    val cancelledAppointmentSeries = appointmentCancelDomainService.cancelAppointments(
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

    return cancelledAppointmentSeries
  }

  fun uncancelAppointment(appointmentId: Long, request: AppointmentUncancelRequest, principal: Principal): AppointmentSeriesModel {
    val startTimeInMs = System.currentTimeMillis()
    val now = LocalDateTime.now()

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val appointmentSeries = appointment.appointmentSeries
    val appointmentsToUncancel = appointmentSeries.applyToAppointments(appointment, request.applyTo, "uncancel", true)
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val uncancelAppointmentsCount = appointmentsToUncancel.size
    val uncancelInstancesCount = appointmentCancelDomainService.getUncancelInstancesCount(appointmentsToUncancel)
    // Determine if this is an uncancel request that will affect more than one appointment and a very large number of appointment instances. If it is, only uncancel the first appointment
    val uncancelFirstAppointmentOnly = uncancelAppointmentsCount > 1 && uncancelInstancesCount > maxSyncAppointmentInstanceActions

    val uncancelledAppointmentSeries = appointmentCancelDomainService.uncancelAppointments(
      appointmentSeries,
      appointmentId,
      if (uncancelFirstAppointmentOnly) setOf(appointment) else appointmentsToUncancel.toSet(),
      request,
      now,
      principal.name,
      uncancelAppointmentsCount,
      uncancelInstancesCount,
      startTimeInMs,
      !uncancelFirstAppointmentOnly,
      true,
    )

    if (uncancelFirstAppointmentOnly) {
      // The remaining appointments will be updated asynchronously by this job
      uncancelAppointmentsJob.execute(
        appointmentSeries.appointmentSeriesId,
        appointmentId,
        appointmentsToUncancel.filterNot { it.appointmentId == appointmentId }.map { it.appointmentId }.toSet(),
        request,
        now,
        principal.name,
        uncancelAppointmentsCount,
        uncancelInstancesCount,
        startTimeInMs,
      )
    }

    return uncancelledAppointmentSeries
  }
}
