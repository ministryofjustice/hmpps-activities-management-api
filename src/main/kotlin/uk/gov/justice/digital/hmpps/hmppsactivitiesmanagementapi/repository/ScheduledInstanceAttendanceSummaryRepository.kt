package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstanceAttendanceSummary
import java.time.LocalDate

@Repository
interface ScheduledInstanceAttendanceSummaryRepository : ReadOnlyRepository<ScheduledInstanceAttendanceSummary, Long> {
  @Query(
    value = """
    FROM ScheduledInstanceAttendanceSummary as 
    WHERE as.prisonCode = :prisonCode
    AND as.sessionDate = :sessionDate
  """,
  )
  fun findByPrisonAndDate(prisonCode: String, sessionDate: LocalDate): List<ScheduledInstanceAttendanceSummary>
}
