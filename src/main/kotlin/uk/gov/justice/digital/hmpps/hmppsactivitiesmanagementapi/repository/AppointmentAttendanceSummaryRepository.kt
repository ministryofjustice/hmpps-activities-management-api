package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendanceSummary
import java.time.LocalDate

@Repository
interface AppointmentAttendanceSummaryRepository : ReadOnlyRepository<AppointmentAttendanceSummary, Long> {
  fun findByPrisonCodeAndStartDate(prisonCode: String, startDate: LocalDate): List<AppointmentAttendanceSummary>
  fun findByPrisonCodeAndStartDateAndCategoryCode(prisonCode: String, startDate: LocalDate, categoryCode: String): List<AppointmentAttendanceSummary>
  fun findByPrisonCodeAndStartDateAndCustomName(prisonCode: String, startDate: LocalDate, customName: String): List<AppointmentAttendanceSummary>
  fun findByPrisonCodeAndStartDateAndCategoryCodeAndCustomName(prisonCode: String, startDate: LocalDate, categoryCode: String, customName: String): List<AppointmentAttendanceSummary>
}
