package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchSpecification
import java.security.Principal

@Service
class AppointmentOccurrenceSearchService(
  private val appointmentOccurrenceSearchRepository: AppointmentOccurrenceSearchRepository,
  private val appointmentOccurrenceSearchSpecification: AppointmentOccurrenceSearchSpecification,
  private val prisonRegimeService: PrisonRegimeService,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
) {
  fun searchAppointmentOccurrences(
    prisonCode: String,
    request: AppointmentOccurrenceSearchRequest,
    principal: Principal,
  ): List<AppointmentOccurrenceSearchResult> {
    var spec = appointmentOccurrenceSearchSpecification.prisonCodeEquals(prisonCode)

    request.appointmentType?.apply {
      spec = spec.and { root, _, cb -> cb.equal(root.get<Long>("appointmentType"), request.appointmentType) }
    }

    spec = if (request.endDate != null) {
      spec.and(appointmentOccurrenceSearchSpecification.startDateBetween(request.startDate!!, request.endDate))
    } else {
      spec.and(appointmentOccurrenceSearchSpecification.startDateEquals(request.startDate!!))
    }

    request.timeSlot?.apply {
      val timeRange = request.timeSlot.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
      spec = spec.and(appointmentOccurrenceSearchSpecification.startTimeBetween(timeRange.start, timeRange.end.minusMinutes(1)))
    }

    request.categoryCode?.apply {
      spec = spec.and(appointmentOccurrenceSearchSpecification.categoryCodeEquals(request.categoryCode))
    }

    request.internalLocationId?.apply {
      spec = spec.and(appointmentOccurrenceSearchSpecification.internalLocationIdEquals(request.internalLocationId))
    }

    request.inCell?.apply {
      spec = spec.and(appointmentOccurrenceSearchSpecification.inCellEquals(request.inCell))
    }

    if (request.prisonerNumbers?.isEmpty() == false) {
      spec = spec.and(appointmentOccurrenceSearchSpecification.prisonerNumbersIn(request.prisonerNumbers))
    }

    request.createdBy?.apply {
      spec = spec.and(appointmentOccurrenceSearchSpecification.createdByEquals(request.createdBy))
    }

    val results = appointmentOccurrenceSearchRepository.findAll(spec)

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)

    return results.toResults(referenceCodeMap, locationMap)
  }
}
