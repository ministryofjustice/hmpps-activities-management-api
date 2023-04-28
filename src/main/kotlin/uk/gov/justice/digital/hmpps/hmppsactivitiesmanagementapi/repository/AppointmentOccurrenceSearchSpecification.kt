package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocationSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceSearch
import java.time.LocalDate
import java.time.LocalTime

@Component
class AppointmentOccurrenceSearchSpecification {
  fun prisonCodeEquals(prisonCode: String) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.equal(root.get<String>("prisonCode"), prisonCode) }

  fun startDateEquals(startDate: LocalDate) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.equal(root.get<LocalDate>("startDate"), startDate) }

  fun startDateBetween(startDate: LocalDate, endDate: LocalDate) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.between(root.get("startDate"), startDate, endDate) }

  fun startTimeBetween(startTime: LocalTime, endTime: LocalTime) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.between(root.get("startTime"), startTime, endTime) }

  fun categoryCodeEquals(categoryCode: String) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.equal(root.get<String>("categoryCode"), categoryCode) }

  fun internalLocationIdEquals(internalLocationId: Long) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.equal(root.get<Long>("internalLocationId"), internalLocationId) }

  fun inCellEquals(inCell: Boolean) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.equal(root.get<Long>("inCell"), inCell) }

  fun prisonerNumbersIn(prisonerNumbers: List<String>) =
    Specification<AppointmentOccurrenceSearch> { root, _, _ -> root.join<AppointmentOccurrenceSearch, AppointmentOccurrenceAllocationSearch>("allocations").get<String>("prisonerNumber").`in`(prisonerNumbers) }

  fun createdByEquals(createdBy: String) =
    Specification<AppointmentOccurrenceSearch> { root, _, cb -> cb.equal(root.get<String>("createdBy"), createdBy) }
}
