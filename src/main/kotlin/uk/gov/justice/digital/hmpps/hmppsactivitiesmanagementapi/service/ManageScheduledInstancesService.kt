package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobDefinition
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.SafeJobRunner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate

@Service
@Transactional
class ManageScheduledInstancesService(
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val activityService: ActivityService,
  private val jobRunner: SafeJobRunner,
  @Value("\${jobs.create-scheduled-instances.days-in-advance}") private val daysInAdvance: Long? = 0L,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /*
  * This finds all prisons that are active in the rollout table, and for each one, finds
  * the activities and schedules that are active between today and SCHEDULE_AHEAD_DAYS days,
  * a value that is set as an environment variable in helm configuration.
  *
  * For each ACTIVITY_SCHEDULE it calls a method to examine the days between now and SCHEDULE_AHEAD_DAYS
  * to check the slots and determine which of these need a scheduled instance to be created.
  *
  * IMPORTANT: It will avoid duplicating scheduled instances for the same day if they already exist, so
  * it is safe to re-run for the same period (or overlapping periods). It will also avoid scheduling for
  * bank holidays, except for those activities that are marked as Ok to run on bank holidays.
  *
  * It will still create scheduled instances where an activity schedule has been suspended between two dates
  * i.e. where a row exists in the ACTIVITY_SCHEDULE_SUSPENSION table because this table should always be
  * empty - there is no current way in the UI to populate it.
  */
  fun create() {
    jobRunner.runJob(
      JobDefinition(
        JobType.SCHEDULES,
      ) {
        val today = LocalDate.now()
        val endDay = today.plusDays(daysInAdvance!!)
        val listOfDatesToSchedule = today.datesUntil(endDay).toList()
        log.info("Scheduling activities job running - from $today until $endDay")

        rolloutPrisonRepository.findAll().filter { it.isActivitiesRolledOut() }.forEach { prison ->
          log.info("Scheduling activities for prison ${prison.description} until $endDay")

          // Get the activities (basic) in this prison that are active between these dates
          val activities = activityRepository.getBasicForPrisonBetweenDates(prison.code, today, endDay)
          activities.forEach { basic ->
            continueToRunOnFailure(
              block = { createInstancesForActivitySchedule(prison, basic.activityScheduleId, listOfDatesToSchedule) },
              success = "Scheduling sessions of ${prison.code} ${basic.summary}",
              failure = "Failed to schedule ${prison.code} ${basic.summary}",
            )
          }
        }
      },
    )
  }

  private fun createInstancesForActivitySchedule(prison: RolloutPrison, scheduleId: Long, days: List<LocalDate>) {
    // Retrieve instances which are for today, or in the future when creating sessions - avoid full-entity object maps
    val earliestSession = LocalDate.now()

    val schedule = activityScheduleRepository.getActivityScheduleByIdWithFilters(scheduleId, earliestSession)
      ?: throw(EntityNotFoundException("Activity schedule ID $scheduleId not found"))

    days.forEach { day ->
      continueToRunOnFailure(
        block = {
          if (schedule.isSuspendedOn(day)) {
            log.info("This instance was suspended ${schedule.description} on $day")
          }

          val lastUpdate = schedule.instancesLastUpdatedTime
          activityService.addScheduleInstances(schedule, days)

          // If there are been updates, save
          if (schedule.instancesLastUpdatedTime != lastUpdate) {
            activityScheduleRepository.saveAndFlush(schedule)
          }
        },
        success = "Scheduling session at ${prison.code} ${schedule.description} on $day",
        failure = "Failed to schedule ${prison.code} ${schedule.description} on $day",
      )
    }
  }

  private fun continueToRunOnFailure(block: () -> Unit, success: String, failure: String) {
    runCatching {
      block()
    }
      .onSuccess { log.info(success) }
      .onFailure { log.error(failure, it) }
  }
}
