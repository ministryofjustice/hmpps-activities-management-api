package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

interface AttendanceRepository : JpaRepository<Attendance, Long> {
  fun existsAttendanceByScheduledInstanceAndPrisonerNumber(scheduledInstance: ScheduledInstance, prisonerNumber: String): Boolean

  @Query(
    """
      SELECT a from Attendance a
      WHERE a.scheduledInstance.activitySchedule.activity.prisonCode = :prisonCode
      AND a.status = 'WAITING'
      AND a.scheduledInstance.sessionDate = :sessionDate
    """,
  )
  fun findWaitingAttendancesOnDate(prisonCode: String, sessionDate: LocalDate): List<Attendance>

  @Query(
    """
      SELECT a
        FROM Attendance a
       WHERE a.scheduledInstance.activitySchedule.activity.prisonCode = :prisonCode
         AND (:attendanceStatus is null or a.status = :attendanceStatus)
         AND a.scheduledInstance.sessionDate >= :sessionDate
         AND a.prisonerNumber = :prisonerNumber
    """,
  )
  fun findAttendancesOnOrAfterDateForPrisoner(prisonCode: String, sessionDate: LocalDate, attendanceStatus: AttendanceStatus? = null, prisonerNumber: String): List<Attendance>

  @Query(
    """
      SELECT a from Attendance a
      WHERE a.scheduledInstance.activitySchedule.activity.activityId = :activityId 
      AND a.scheduledInstance.sessionDate = :sessionDate
    """,
  )
  fun findAttendancesForActivityOnDate(activityId: Long, sessionDate: LocalDate): List<Attendance>
}
