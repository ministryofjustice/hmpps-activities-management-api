package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
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
  private val prisonApiClient: PrisonApiClient,
) {
  fun getAppointmentSeriesDetailsById(appointmentSeriesId: Long): AppointmentSeriesDetails {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    checkCaseloadAccess(appointmentSeries.prisonCode)

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode)

    val userMap = prisonApiClient.getUserDetailsList(appointmentSeries.usernames()).associateBy { it.username }

    return appointmentSeries.toDetails(referenceCodeMap, locationMap, userMap)
  }
}
