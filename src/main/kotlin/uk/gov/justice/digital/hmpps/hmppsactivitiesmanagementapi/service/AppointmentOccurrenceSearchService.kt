package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchRepository
import java.security.Principal

@Service
class AppointmentOccurrenceSearchService(
  private val prisonRegimeService: PrisonRegimeService,
  private val appointmentOccurrenceSearchRepository: AppointmentOccurrenceSearchRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
) {
  fun searchAppointmentOccurrences(
    prisonCode: String,
    request: AppointmentOccurrenceSearchRequest,
    principal: Principal,
  ): List<AppointmentOccurrenceSearchResult> {
    val timeRange = request.timeSlot?.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }

    val results = appointmentOccurrenceSearchRepository.find(prisonCode, request.categoryCode, request.internalLocationId, request.startDate, timeRange?.start, timeRange?.end)

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)

    return results.toResults(referenceCodeMap, locationMap)
  }
}
