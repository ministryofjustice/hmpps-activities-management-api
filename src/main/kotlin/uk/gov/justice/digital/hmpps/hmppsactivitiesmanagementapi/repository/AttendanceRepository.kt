package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

interface AttendanceRepository : JpaRepository<Attendance, Long> {
  fun existsAttendanceByScheduledInstanceAndPrisonerNumber(scheduledInstance: ScheduledInstance, prisonerNumber: String): Boolean

  /**
   * Caution should be used with this query.
   *
   * Over time the number of attendance records will be significant so the date boundaries used should not be large.
   */
  @Query(
    """
      SELECT a from Attendance a
      WHERE a.scheduledInstance.activitySchedule.activity.prisonCode = :prisonCode
      AND a.status != 'LOCKED'
      AND a.scheduledInstance.sessionDate >= :startDate
      AND a.scheduledInstance.sessionDate <= :endDate
    """,
  )
  fun findUnlockedAttendancesAtPrisonBetweenDates(prisonCode: String, startDate: LocalDate, endDate: LocalDate): List<Attendance>
}
