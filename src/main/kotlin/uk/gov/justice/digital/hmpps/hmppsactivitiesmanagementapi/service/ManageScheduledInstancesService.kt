package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobDefinition
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.SafeJobRunner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageScheduledInstancesService(
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val bankHolidayService: BankHolidayService,
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

          // Get the activities in this prison that are active between these dates
          val activities = activityRepository.getAllForPrisonBetweenDates(prison.code, today, endDay)
          val activitySchedules = activities.map {
            it.schedules().filter { s ->
              s.startDate.isBefore(endDay) && (s.endDate == null || s.endDate!!.isAfter(today))
            }
          }.flatten()

          // For each active schedule add any new scheduled instances as required
          activitySchedules.forEach { schedule ->
            continueToRunOnFailure(
              block = { createInstancesForSchedule(prison, schedule.activityScheduleId, listOfDatesToSchedule) },
              success = "Scheduling sessions of ${prison.code} ${schedule.description}",
              failure = "Failed to schedule ${prison.code} ${schedule.description}",
            )
          }
        }
      },
    )
  }

  private fun createInstancesForSchedule(prison: RolloutPrison, scheduleId: Long, days: List<LocalDate>) {
    var instancesCreated = false
    val schedule = activityScheduleRepository.findById(scheduleId).orElseThrow {
      EntityNotFoundException("Activity schedule ID $scheduleId not found")
    }

    days.forEach { day ->
      if (schedule.isActiveOn(day) && schedule.canBeScheduledOnDay(day) && schedule.hasNoInstancesOnDate(day)) {
        val filteredSlots = schedule.slots().filter { slot -> day.dayOfWeek in slot.getDaysOfWeek() }
        filteredSlots.forEach { slot ->
          continueToRunOnFailure(
            block = { schedule.addInstance(sessionDate = day, slot = slot) },
            success = "Scheduling session at ${prison.code} ${schedule.description} on $day at ${slot.startTime}",
            failure = "Failed to schedule ${prison.code} ${schedule.description} on $day at ${slot.startTime}",
          )

          // Ignoring planned suspensions for now - logged for interest (no UI way to set these yet)
          if (schedule.isSuspendedOn(day)) {
            log.info("This instance was suspended ${schedule.description} on $day at ${slot.startTime}")
          }

          instancesCreated = true
        }
      }
    }

    if (instancesCreated) {
      // This will trigger the sync event
      schedule.instancesLastUpdatedTime = LocalDateTime.now()
      activityScheduleRepository.saveAndFlush(schedule)
    }
  }

  private fun continueToRunOnFailure(block: () -> Unit, success: String, failure: String) {
    runCatching {
      block()
    }
      .onSuccess { log.info(success) }
      .onFailure { log.error(failure, it) }
  }

  private fun ActivitySchedule.canBeScheduledOnDay(day: LocalDate) =
    this.runsOnBankHoliday || !bankHolidayService.isEnglishBankHoliday(day)
}
