package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class AppointmentOccurrenceDetailsService(
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentOccurrenceDetailsById(appointmentOccurrenceId: Long): AppointmentOccurrenceDetails {
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment

    val categorySummary = appointment.category.toSummary()

    val prisonCode = appointment.prisonCode

    val locationMap = appointmentOccurrence.internalLocationId?.let { locationService.getLocationsForAppointmentsMap(appointment.prisonCode, listOf(it)) } ?: emptyMap()

    val userMap = prisonApiClient.getUserDetailsList(appointment.usernames()).associateBy { it.username }

    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(appointmentOccurrence.prisonerNumbers()).block()!!

    return appointmentOccurrence.toDetails(categorySummary, prisonCode, locationMap, userMap, prisoners)
  }
}
