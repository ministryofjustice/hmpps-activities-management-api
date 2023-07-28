package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal

@Service
@Transactional(readOnly = true)
class AppointmentOccurrenceSearchService(
  private val appointmentOccurrenceSearchRepository: AppointmentOccurrenceSearchRepository,
  private val appointmentOccurrenceAllocationSearchRepository: AppointmentOccurrenceAllocationSearchRepository,
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
    checkCaseloadAccess(prisonCode)

    var spec = appointmentOccurrenceSearchSpecification.prisonCodeEquals(prisonCode)

    with(request) {
      appointmentType?.apply {
        spec = spec.and { root, _, cb -> cb.equal(root.get<Long>("appointmentType"), appointmentType) }
      }

      spec = if (endDate != null) {
        spec.and(appointmentOccurrenceSearchSpecification.startDateBetween(startDate!!, endDate))
      } else {
        spec.and(appointmentOccurrenceSearchSpecification.startDateEquals(startDate!!))
      }

      timeSlot?.apply {
        val timeRange = timeSlot.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
        spec = spec.and(
          appointmentOccurrenceSearchSpecification.startTimeBetween(
            timeRange.start,
            timeRange.end.minusMinutes(1),
          ),
        )
      }

      categoryCode?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.categoryCodeEquals(categoryCode))
      }

      internalLocationId?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.internalLocationIdEquals(internalLocationId))
      }

      inCell?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.inCellEquals(inCell))
      }

      if (prisonerNumbers?.isEmpty() == false) {
        spec = spec.and(appointmentOccurrenceSearchSpecification.prisonerNumbersIn(prisonerNumbers))
      }

      createdBy?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.createdByEquals(createdBy))
      }
    }

    val results = appointmentOccurrenceSearchRepository.findAll(spec)

    val allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(results.map { it.appointmentOccurrenceId })
      .groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)

    return results.toResults(allocationsMap, referenceCodeMap, locationMap)
  }
}
