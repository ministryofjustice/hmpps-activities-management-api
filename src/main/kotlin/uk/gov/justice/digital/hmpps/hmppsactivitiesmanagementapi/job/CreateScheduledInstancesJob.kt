package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.BankHolidayService
import java.time.LocalDate

@Component
class CreateScheduledInstancesJob(
  private val activityRepository: ActivityRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val bankHolidayService: BankHolidayService,
  @Value("\${jobs.create-scheduled-instances.days-in-advance}") private val daysInAdvance: Long? = 0L
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /*
  * This job finds all prisons that are active in the rollout table, and for each one, finds
  * the activities and schedules that are active at that prison between today SCHEDULE_AHEAD_DAYS days
  * into the future. This value is set as an environment variable in the helm configuration.
  *
  * For each day between the two dates it looks at the list of activities in a prison, their schedules and
  * slots, and determine which ones need a scheduled instance creating for them. It will avoid duplicating
  * instances for the same day - so safe to re-run for the same period (or overlapping periods). Will need
  * some thought here to pick up changes made to scheduled instances that already exist? These will need to
  * be amended in-situ by the user action of changing a schedule or slot (i.e. not the responsibility of this job).
  *
  * It will also avoid bank holidays, except for those activities that are marked as running on bank holidays.
  * It will avoid creating scheduled instances where an activity has been cancelled:
  * BUT - if cancelled activities are paid, we will still need to create them and mark them as cancelled.
  *     - the job which creates attendances will also need some logic to deal with these.
  * AND THESE WILL NEED TO BE CATERED FOR WHEN REQUIREMENTS ARE CLEARER
  */
  @Async("asyncExecutor")
  @Transactional
  fun execute() {
    val today = LocalDate.now()
    val endDay = today.plusDays(daysInAdvance!!)

    log.info("Scheduling activities job running - from $today until $endDay")

    rolloutPrisonRepository.findAll().filter { it.active }.forEach { prison ->

      log.info("Scheduling activities for prison ${prison.description} until $endDay")

      val activities = activityRepository.getAllForPrisonBetweenDates(prison.code, today, endDay)
      val listOfDatesToSchedule = today.datesUntil(endDay).toList()

      listOfDatesToSchedule.forEach { day ->
        val filteredForDay = activities.filterIsActiveOnDay(day)

        filteredForDay.forEach { activity ->
          var activityChanged = false
          val activeAndSuspendedSchedules = activity.getSchedulesOnDay(day, includeSuspended = true)
          val withNoPreExistingInstances = activeAndSuspendedSchedules.filterActivitySchedulesWithNoPreExistingInstance(day)

          withNoPreExistingInstances.forEach { schedule ->
            val filteredSlots = schedule.slots().filter { day.dayOfWeek in it.getDaysOfWeek() }
            filteredSlots.filterActivityScheduleSlotsForBankHoliday(day).forEach { slot ->
              continueToRunOnFailure(
                block = {
                  schedule.addInstance(sessionDate = day, slot = slot)
                  activityChanged = true
                },
                success = "Scheduling activity at ${prison.code} ${activity.summary} on $day at ${slot.startTime}",
                failure = "Failed to schedule activity at ${prison.code} ${activity.summary} on $day at ${slot.startTime} for schedule ${schedule.description}"
              )
            }
          }
          if (activityChanged) {
            continueToRunOnFailure(
              block = { activityRepository.save(activity) },
              success = "Scheduled activity at ${prison.code} ${activity.summary} on $day",
              failure = "Failed to schedule activity at ${prison.code} ${activity.summary} on $day",
            )
          }
        }
      }
    }
  }

  // TODO meed to decide how we intend to monitor/alert failures!
  private fun continueToRunOnFailure(block: () -> Unit, success: String, failure: String) {
    runCatching {
      block()
    }
      .onSuccess { log.info(success) }
      .onFailure { log.error(failure, it) }
  }

  private fun List<ActivityScheduleSlot>.filterActivityScheduleSlotsForBankHoliday(day: LocalDate) =
    this.filter { it.runsOnBankHoliday || !bankHolidayService.isEnglishBankHoliday(day) }

  private fun List<ActivitySchedule>.filterActivitySchedulesWithNoPreExistingInstance(day: LocalDate) =
    this.filter { it.instances().none { scheduledInstance -> scheduledInstance.sessionDate == day } }

  private fun List<Activity>.filterIsActiveOnDay(day: LocalDate) =
    this.filter { it.isActive(day) }
}
