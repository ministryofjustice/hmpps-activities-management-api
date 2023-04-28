package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class AppointmentDetailsService(
  private val appointmentRepository: AppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentDetailsById(appointmentId: Long): AppointmentDetails {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)

    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers()).block()!!

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointment.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointment.usernames()).associateBy { it.username }

    return appointment.toDetails(prisoners, referenceCodeMap, locationMap, userMap)
  }
}
