package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import java.time.LocalDate

@Component
class AppointmentSeriesSpecification {
  fun prisonCodeEquals(prisonCode: String) = Specification<AppointmentSeries> { root, _, cb -> cb.equal(root.get<String>("prisonCode"), prisonCode) }

  fun startDateGreaterThanOrEquals(startDate: LocalDate) = Specification<AppointmentSeries> { root, _, cb -> cb.greaterThanOrEqualTo(root.get("startDate"), startDate) }

  fun categoryCodeEquals(categoryCode: String) = Specification<AppointmentSeries> { root, _, cb -> cb.equal(root.get<String>("categoryCode"), categoryCode) }

  fun isMigrated() = Specification<AppointmentSeries> { root, _, cb -> cb.equal(root.get<Boolean>("isMigrated"), true) }
}
