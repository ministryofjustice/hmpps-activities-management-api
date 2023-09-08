package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess

// TODO: merge with AppointmentService
@Service
@Transactional(readOnly = true)
class AppointmentDetailsService(
  private val appointmentRepository: AppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentDetailsById(appointmentId: Long): AppointmentOccurrenceDetails {
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.appointmentSeries.prisonCode)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(appointment.prisonerNumbers())

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointment.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointment.usernames()).associateBy { it.username }

    return appointment.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)
  }
}
