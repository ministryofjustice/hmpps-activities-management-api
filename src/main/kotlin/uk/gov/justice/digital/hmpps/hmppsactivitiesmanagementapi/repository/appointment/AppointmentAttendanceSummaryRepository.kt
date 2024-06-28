package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ReadOnlyRepository
import java.time.LocalDate

@Repository
interface AppointmentAttendanceSummaryRepository : ReadOnlyRepository<AppointmentAttendanceSummary, Long> {
  fun findByPrisonCodeAndStartDate(prisonCode: String, startDate: LocalDate): List<AppointmentAttendanceSummary>
  fun findByPrisonCodeAndStartDateAndCategoryCode(prisonCode: String, startDate: LocalDate, categoryCode: String): List<AppointmentAttendanceSummary>
  fun findByPrisonCodeAndStartDateAndCustomName(prisonCode: String, startDate: LocalDate, customName: String): List<AppointmentAttendanceSummary>
  fun findByPrisonCodeAndStartDateAndCategoryCodeAndCustomName(prisonCode: String, startDate: LocalDate, categoryCode: String, customName: String): List<AppointmentAttendanceSummary>
}
