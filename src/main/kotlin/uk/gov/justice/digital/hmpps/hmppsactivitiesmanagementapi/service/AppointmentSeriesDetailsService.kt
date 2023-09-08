package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess

// TODO: merge with AppointmentSeriesService
@Service
@Transactional(readOnly = true)
class AppointmentSeriesDetailsService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentSeriesDetailsById(appointmentSeriesId: Long): AppointmentDetails {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(appointmentSeries.prisonerNumbers()).block()!!

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointmentSeries.usernames()).associateBy { it.username }

    return appointmentSeries.toDetails(prisoners, referenceCodeMap, locationMap, userMap)
  }
}
