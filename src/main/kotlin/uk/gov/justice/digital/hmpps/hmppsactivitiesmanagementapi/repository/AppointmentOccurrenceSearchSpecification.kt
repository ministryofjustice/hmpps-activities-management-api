package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendeeSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSearch
import java.time.LocalDate
import java.time.LocalTime

@Component
class AppointmentOccurrenceSearchSpecification {
  fun prisonCodeEquals(prisonCode: String) =
    Specification<AppointmentSearch> { root, _, cb -> cb.equal(root.get<String>("prisonCode"), prisonCode) }

  fun startDateEquals(startDate: LocalDate) =
    Specification<AppointmentSearch> { root, _, cb -> cb.equal(root.get<LocalDate>("startDate"), startDate) }

  fun startDateBetween(startDate: LocalDate, endDate: LocalDate) =
    Specification<AppointmentSearch> { root, _, cb -> cb.between(root.get("startDate"), startDate, endDate) }

  fun startTimeBetween(startTime: LocalTime, endTime: LocalTime) =
    Specification<AppointmentSearch> { root, _, cb -> cb.between(root.get("startTime"), startTime, endTime) }

  fun categoryCodeEquals(categoryCode: String) =
    Specification<AppointmentSearch> { root, _, cb -> cb.equal(root.get<String>("categoryCode"), categoryCode) }

  fun internalLocationIdEquals(internalLocationId: Long) =
    Specification<AppointmentSearch> { root, _, cb -> cb.equal(root.get<Long>("internalLocationId"), internalLocationId) }

  fun inCellEquals(inCell: Boolean) =
    Specification<AppointmentSearch> { root, _, cb -> cb.equal(root.get<Long>("inCell"), inCell) }

  fun prisonerNumbersIn(prisonerNumbers: List<String>) =
    Specification<AppointmentSearch> { root, _, _ -> root.join<AppointmentSearch, AppointmentAttendeeSearch>("attendees").get<String>("prisonerNumber").`in`(prisonerNumbers) }

  fun createdByEquals(createdBy: String) =
    Specification<AppointmentSearch> { root, _, cb -> cb.equal(root.get<String>("createdBy"), createdBy) }
}
