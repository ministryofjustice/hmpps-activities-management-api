package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchRepository
import java.security.Principal
import java.time.LocalDate

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
    var spec = Specification<AppointmentOccurrenceSearch>{ root, _, cb -> cb.equal(root.get<String>("prisonCode"), prisonCode) }

    request.categoryCode?.apply {
      spec = spec.and { root, _, cb -> cb.equal(root.get<String>("categoryCode"), request.categoryCode) }
    }

    request.internalLocationId?.apply {
      spec = spec.and { root, _, cb -> cb.equal(root.get<Long>("internalLocationId"), request.internalLocationId) }
    }

    spec = spec.and { root, _, cb -> cb.equal(root.get<LocalDate>("startDate"), request.startDate!!) }

    request.timeSlot?.apply {
      val timeRange = request.timeSlot.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
      spec = spec.and { root, _, cb -> cb.between(root.get("startTime"), timeRange.start, timeRange.end) }
    }

    val results = appointmentOccurrenceSearchRepository.findAll(spec)

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)

    return results.toResults(referenceCodeMap, locationMap)
  }
}
