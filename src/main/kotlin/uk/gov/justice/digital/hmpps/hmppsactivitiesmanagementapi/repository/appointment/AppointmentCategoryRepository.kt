package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentCategory
import java.util.Optional

@Repository
interface AppointmentCategoryRepository : JpaRepository<AppointmentCategory, Long> {
  fun findByCode(code: String): Optional<AppointmentCategory>
}
