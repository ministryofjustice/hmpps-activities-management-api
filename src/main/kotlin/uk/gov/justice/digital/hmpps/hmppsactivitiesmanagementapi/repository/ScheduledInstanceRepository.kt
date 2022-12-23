package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityForPrisonerProjection
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import java.time.LocalDate

// TODO: Change all of these to use the same DB View  - V_ACTIVITIES_FOR_PRISONERS
// It saves many different mappings between scheduled instances and the projections.

interface ScheduledInstanceRepository : JpaRepository<ScheduledInstance, Long> {
  @Query(
    "SELECT si FROM ScheduledInstance si WHERE EXISTS(" +
      "SELECT 1 FROM si.activitySchedule.allocations a " +
      "WHERE a.activitySchedule.activity.prisonCode = :prisonCode " +
      "AND a.prisonerNumber = :prisonerNumber " +
      "AND a.startDate <= :date " +
      "AND (a.endDate is null or a.endDate > :date)) " +
      "AND si.sessionDate >= :startDate " +
      "AND si.sessionDate <= :endDate " +
      "AND si.cancelled <> true"
  )
  fun getActivityScheduleInstancesByPrisonerNumberAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): List<ScheduledInstance>

  @Query(
    "SELECT si FROM ScheduledInstance si WHERE EXISTS(" +
      "SELECT 1 FROM si.activitySchedule.allocations a " +
      "WHERE a.activitySchedule.activity.prisonCode = :prisonCode " +
      "AND a.startDate <= :date " +
      "AND (a.endDate is null or a.endDate > :date)) " +
      "AND si.sessionDate >= :startDate " +
      "AND si.sessionDate <= :endDate " +
      "AND si.cancelled <> true"
  )
  fun getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): List<ScheduledInstance>

  fun findAllBySessionDate(date: LocalDate): List<ScheduledInstance>

  // JPA Way for a prisoner list - but models only extract ActivityScheduleLite and ActivityLite :-(
  @Query(
    "SELECT si FROM ScheduledInstance si WHERE EXISTS(" +
      "SELECT 1 FROM si.activitySchedule.allocations a " +
      "WHERE a.activitySchedule.activity.prisonCode = :prisonCode " +
      "AND a.prisonerNumber IN :prisonerNumbers " +
      "AND a.startDate <= :date " +
      "AND (a.endDate is null or a.endDate > :date )) " +
      "AND si.sessionDate = :date " +
      "AND si.cancelled <> true"
  )
  fun getActivityScheduleInstancesByPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
  ): List<ScheduledInstance>

  // Native query to get exactly the rows we want as a custom projection

  @Query(
    """
    SELECT si.scheduled_instance_id,
           si.session_date,
           si.start_time,
           si.end_time,
           alloc.prisoner_number,
           schedule.internal_location_id,
           schedule.internal_location_code,
           schedule.internal_location_description,
           schedule.description as scheduleDescription,
           act.category as activityCategory,
           act.summary as activitySummary,
           act.description as activityDescription
      FROM scheduled_instance si, activity_schedule schedule, allocation alloc, activity act
      WHERE si.session_date = :date
      AND si.cancelled != true
      AND schedule.activity_schedule_id = si.activity_schedule_id,
      AND alloc.activity_schedule_id = si.activity_schedule_id
      AND alloc.prisoner_number in :prisonerNumbers
      AND act.activity_id = schedule.activity_id
      AND act.prison_code = :prisonCode
      AND alloc.startDate <= :date 
      AND (alloc.endDate is null or alloc.endDate > :date)
      """,
    nativeQuery = true
  )
  fun getActivitiesForPrisonerList(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
  ): List<ActivityForPrisonerProjection>
}
