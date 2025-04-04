package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import java.time.LocalDate

@Component
class AppointmentSpecification {
  fun startDateBetween(startDate: LocalDate, endDate: LocalDate) = Specification<Appointment> { root, _, cb -> cb.between(root.get("startDate"), startDate, endDate) }
}
