package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.BankHolidayService
import java.time.LocalDate


@Component
class CreateActivitySessionsJob(
  private val activityRepository: ActivityRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val bankHolidayService: BankHolidayService,
  @Value("\${jobs.create-activity-sessions.days-in-advance}") private val daysInAdvance: Long? = 0L
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /*
  * This job finds all prisons that are active in the rollout table, and for each one, finds
  * the activities that are active at that prison between today SCHEDULE_AHEAD_DAYS days
  * into the future. This value is set as an environment variable in the helm configuration.
  *
  * For each day between the two dates it looks the list of activities in a prison, their schedules and
  * slots, and determine which ones need a scheduled instance creating for them. It will avoid duplicating
  * instances for the same day - so safe to re-run for the same period (or overlapping periods). Will need
  * some thought here to pick up changes made to scheduled instances that already exist? These will need to
  * be amended in-situ by the user action of changing a schedule or slot (i.e. not the responsibility of this job).
  *
  * It will also avoid bank holidays, except for those activities that are marked as running on bank holidays.
  * It will avoid creating scheduled instances where an activity has planned suspensions.
  *
  */

  @Async("asyncExecutor")
  fun execute() {
    val today = LocalDate.now()
    val endDay = LocalDate.now().plusDays(daysInAdvance!!)

    log.info("Scheduling activities job running - from $today until $endDay")

    rolloutPrisonRepository.findAll().forEach { prison ->
      if (prison.active) {
        log.info("Scheduling activities for prison ${prison.description}")

        val activityList = activityRepository.getAllForPrisonBetweenDates(prison.code, today, endDay)
        val listOfDatesToSchedule = today.datesUntil(endDay).toList()

        listOfDatesToSchedule.forEach { day ->
          val filteredForDay = activityList.filterIsActiveOnDay(day)

          filteredForDay.forEach { activity ->
            var activityChanged = false

            val schedules = activity.getSchedulesOnDay(day, includeSuspended = false)
            val withNoPreExistingInstances = schedules.filterActivitySchedulesWithNoPreExistingInstance(day)

            withNoPreExistingInstances.forEach { schedule ->
              val filteredSlots = schedule.slots.filter { day.dayOfWeek in it.getDaysOfWeek() }
              val withNoBankHols = filteredSlots.filterActivityScheduleSlotsForBankHoliday(day)

              withNoBankHols.forEach { slot ->
                schedule.instances.add(ScheduledInstance(activitySchedule = schedule, sessionDate = day, startTime = slot.startTime, endTime = slot.endTime))
                activityChanged = true
                log.info("Scheduling activity at ${prison.code} ${activity.summary} on $day at ${slot.startTime}")
              }
            }

            if (activityChanged) {
              activityRepository.save(activity)
            }
          }
        }
      }
    }
  }

  private fun List<ActivityScheduleSlot>.filterActivityScheduleSlotsForBankHoliday(day: LocalDate) =
    this.filter { it.runsOnBankHoliday || !bankHolidayService.isEnglishBankHoliday(day) }

  private fun List<ActivitySchedule>.filterActivitySchedulesWithNoPreExistingInstance(day: LocalDate) =
    this.filter { it.instances.none { scheduledInstance -> scheduledInstance.sessionDate == day } }

  private fun List<Activity>.filterIsActiveOnDay(day: LocalDate) =
    this.filter { it.isActive(day) }
}
