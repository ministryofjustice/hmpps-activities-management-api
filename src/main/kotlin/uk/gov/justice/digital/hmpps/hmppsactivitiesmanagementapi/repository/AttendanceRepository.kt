package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BookingCount
import java.time.LocalDate
import java.time.LocalTime

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
       WHERE a.scheduledInstance.activitySchedule.activityScheduleId = :activityScheduleId
         AND (:attendanceStatus is null or a.status = :attendanceStatus)
         AND a.scheduledInstance.sessionDate >= :sessionDate
         AND a.prisonerNumber = :prisonerNumber
    """,
  )
  fun findAttendancesOnOrAfterDateForAllocation(sessionDate: LocalDate, activityScheduleId: Long, attendanceStatus: AttendanceStatus? = null, prisonerNumber: String): List<Attendance>

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
  fun mergeOldPrisonerNumberToNew(oldNumber: String, newNumber: String)

  @Query(
    value = """
      SELECT 
       att.prisoner_number,
       si.start_time,
       si.end_time,
       a.in_cell,
       a.off_wing,
       a.on_wing,
       acts.internal_location_description,
       si.scheduled_instance_id,
       attr.code AS attendance_reason_code
      FROM scheduled_instance si
      JOIN activity_schedule acts ON acts.activity_schedule_id = si.activity_schedule_id
      JOIN activity a ON a.activity_id = acts.activity_id
      JOIN attendance att ON si.scheduled_instance_id = att.scheduled_instance_id
      JOIN attendance_reason attr ON att.attendance_reason_id = attr.attendance_reason_id
      WHERE a.prison_code = :prisonCode AND si.session_date = :date
       AND attr.code IN ('SUSPENDED', 'AUTO_SUSPENDED') 
       AND (:reason IS NULL OR attr.code = :reason)
      """,
    nativeQuery = true,
  )
  fun getSuspendedPrisonerAttendance(
    @Param("prisonCode") prisonCode: String,
    @Param("date") date: LocalDate,
    @Param("reason") reason: String?,
  ): List<SuspendedPrisonerAttendance>

  @Query(
    value = """
      SELECT 
       time_slot,
       name as category_name,
       summary as activity_summary,
       scheduled_instance_id
       FROM v_activity_time_slot
       WHERE code in :categories
       AND prison_code = :prisonCode
       AND session_date = :date
      """,
    nativeQuery = true,
  )
  fun getActivityTimeSlot(
    @Param("prisonCode") prisonCode: String,
    @Param("date") date: LocalDate,
    @Param("categories") categories: List<String>,
  ): List<ActivityTimeSlot>
}

interface SuspendedPrisonerAttendance {
  fun getPrisonerNumber(): String
  fun getStartTime(): LocalTime
  fun getEndTime(): LocalTime
  fun getInCell(): Boolean
  fun getOffWing(): Boolean
  fun getOnWing(): Boolean
  fun getInternalLocation(): String?
  fun getScheduledInstanceId(): Long
  fun getAttendanceReasonCode(): String
}

interface ActivityTimeSlot {
  fun getCategoryName(): String
  fun getScheduledInstanceId(): Long
  fun getTimeSlot(): String
  fun getActivitySummary(): String
}
