package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class AppointmentDetailService(
  private val appointmentRepository: AppointmentRepository,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentDetailById(appointmentId: Long): AppointmentDetail {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointment.prisonCode, appointment.internalLocationIds())!!

    val userMap = prisonApiClient.getUserDetailsList(appointment.usernames()).block()!!.associateBy { it.username }

    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers()).block()!!

    return appointment.toDetail(locationMap, userMap, prisoners)
  }
}
