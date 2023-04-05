package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  fun findAllBySessionDate(date: LocalDate): List<ScheduledInstance>

  // TODO - should it check for suspensions? Or done in the client? (I added the allocation date checks)

  @Query(
    """
    SELECT si FROM ScheduledInstance si 
    WHERE EXISTS (
      SELECT 1 FROM si.activitySchedule s
      WHERE s.activity.prisonCode = :prisonCode 
      AND si.sessionDate >= :startDate
      AND si.sessionDate <= :endDate
    )
    """,
  )
  fun getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<ScheduledInstance>

  @Query(
    """
    select previoussi
      from scheduledInstance previoussi 
      where concat(sessionDate, startTime) = 
        (select max(concat(sessionDate, startTime))
         from scheduledInstance si2
         where si2.activityScheduleId = previoussi.activityScheduleId
         and concat(si2.sessionDate, si2.startTime) < (select concat(currentsi.sessionDate, currentsi.startTime)
                                  from scheduledInstance currentsi
                                  where currentsi.scheduledInstanceId = :scheduledInstanceId)
         and previoussi.activityScheduleId = (select activityScheduleId
                                  from scheduledInstance currentsi
                                  where currentsi.scheduledInstanceId = :scheduledInstanceId)
        )
    """,
  )
  fun getPreviousScheduledInstance(
    scheduledInstanceId: Long,
  ): ScheduledInstance?

  @Query(
    """
    select *
    from scheduled_instance nextsi 
    where concat(session_date, start_time) = 
      (select min(concat(session_date, start_time))
       from scheduled_instance si2
       where si2.activity_schedule_id = nextsi.activity_schedule_id
       and concat(si2.session_date, si2.start_time) > (select concat(currentsi.session_date, currentsi.start_time)
                                from scheduled_instance currentsi
                                where currentsi.scheduled_instance_id = :scheduledInstanceId)
       and nextsi.activity_schedule_id = (select activity_schedule_id
                                from scheduled_instance currentsi
                                where currentsi.scheduled_instance_id = :scheduledInstanceId)
      )
    """,
    nativeQuery = true,
  )
  fun getNextScheduledInstance(
    scheduledInstanceId: Long,
  ): ScheduledInstance?
}
