package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AllAttendance
import java.time.LocalDate
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface AllAttendanceRepository : ReadOnlyRepository<AllAttendance, Long> {
  fun findByPrisonCodeAndSessionDate(prisonCode: String, sessionDate: LocalDate): List<AllAttendance>
  fun findByPrisonCodeAndSessionDateAndEventTier(prisonCode: String, sessionDate: LocalDate, eventTier: String): List<AllAttendance>

  @Query(
    """
    SELECT aa
     FROM AllAttendance aa
     WHERE aa.prisonerNumber = :prisonerNumber
       AND aa.sessionDate >= :fromDate AND aa.sessionDate <= :toDate
       AND aa.status = 'COMPLETED'
       AND aa.attendanceReasonCode is not null
    """,
  )
  fun findAttendanceBy(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate): List<AllAttendance>
}
