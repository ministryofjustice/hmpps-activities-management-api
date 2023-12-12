package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BookingCount
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

  @Query(
    value = """
      select new uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BookingCount(al.bookingId, count(att)) from Attendance att 
      join ScheduledInstance si on att.scheduledInstance = si
      join ActivitySchedule asch on si.activitySchedule = asch
      join Allocation al on al.activitySchedule = asch and att.prisonerNumber = al.prisonerNumber and al.startDate <= : date and (al.endDate is null or al.endDate >= :date)
      join Activity act on asch.activity = act
      where si.sessionDate = :date
      and act.prisonCode = :prisonCode
      and att.issuePayment = true
      group by al.bookingId
      order by al.bookingId
    """,
  )
  fun findBookingPaidAttendanceCountsByPrisonAndDate(
    prisonCode: String,
    date: LocalDate,
  ): List<BookingCount>

  fun findByPrisonerNumber(prisonerNumber: String): List<Attendance>

  @Query(value = "UPDATE Attendance a SET a.prisonerNumber = :newNumber WHERE a.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOffender(oldNumber: String, newNumber: String)
}
