package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate
import java.time.LocalTime

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  @Query(
    """
    SELECT si FROM ScheduledInstance si 
    WHERE EXISTS (
      SELECT 1 FROM si.activitySchedule s
      WHERE s.activity.prisonCode = :prisonCode 
      AND si.sessionDate >= :startDate
      AND si.sessionDate <= :endDate
      AND (:timeSlot is null or si.timeSlot = :timeSlot)
    ) AND (:cancelled is null or si.cancelled = :cancelled)
    """,
  )
  fun getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    cancelled: Boolean? = null,
    timeSlot: TimeSlot? = null,
  ): List<ScheduledInstance>

  @Query(
    """
    SELECT si FROM ScheduledInstance si
    WHERE si.activitySchedule.activity.prisonCode = :prisonCode
    AND si.sessionDate >= :startDate
    AND si.sessionDate <= :endDate
    AND (:cancelled is null or si.cancelled = :cancelled)
    AND (:timeSlot is null or si.timeSlot = :timeSlot)
    AND (
      EXISTS (
        SELECT 1 FROM si.attendances a WHERE a.prisonerNumber = :prisonerNumber
      )
      OR
      EXISTS (
        SELECT 1 FROM si.advanceAttendances aa WHERE aa.prisonerNumber = :prisonerNumber
      )
    )
    """,
  )
  fun getActivityScheduleInstancesForPrisonerByPrisonCodeAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate,
    cancelled: Boolean? = null,
    timeSlot: TimeSlot? = null,
  ): List<ScheduledInstance>

  @Query(
    "select si " +
      "from ScheduledInstance si " +
      "join fetch si.activitySchedule asch " +
      "join fetch asch.activity act " +
      "join fetch act.activityCategory actc " +
      "left join fetch si.attendances att " +
      "left join fetch att.attendanceReason attr " +
      "where si.scheduledInstanceId in :ids",
  )
  fun findByIds(ids: List<Long>): List<ScheduledInstance>

  fun findByActivityScheduleAndSessionDateEqualsAndStartTimeGreaterThanEqual(
    activitySchedule: ActivitySchedule,
    sessionDate: LocalDate,
    startTime: LocalTime,
  ): List<ScheduledInstance>
}
