package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.custom.AttendanceSummary
import java.time.LocalDate
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AllAttendanceRepository : ReadOnlyRepository<AllAttendance, Long> {
  fun findByPrisonCodeAndSessionDate(prisonCode: String, sessionDate: LocalDate): List<AllAttendance>

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.custom.AttendanceSummary(aa.attendanceReasonCode, COUNT(aa.attendanceId))
     FROM AllAttendance aa
     WHERE aa.prisonerNumber = :prisonerNumber
       AND aa.sessionDate >= :fromDate AND aa.sessionDate <= :toDate
       AND aa.status = 'COMPLETED'
       GROUP BY aa.attendanceReasonCode
    """,
  )
  fun findAttendanceSummaryBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate): List<AttendanceSummary>
}
