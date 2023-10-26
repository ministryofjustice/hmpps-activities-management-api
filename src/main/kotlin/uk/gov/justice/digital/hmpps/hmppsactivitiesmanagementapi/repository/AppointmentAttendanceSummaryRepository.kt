package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendanceSummary
import java.time.LocalDate

@Repository
interface AppointmentAttendanceSummaryRepository : ReadOnlyRepository<AppointmentAttendanceSummary, Long> {
  fun findByPrisonCodeAndStartDate(prisonCode: String, startDate: LocalDate): List<AppointmentAttendanceSummary>
}
