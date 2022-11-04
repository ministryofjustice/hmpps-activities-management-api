package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import java.time.LocalDate

@Component
class CreateActivitySessionsJob(
  private val activityRepository: ActivityRepository,
  @Value("\${jobs.create-activity-sessions.days-in-advance}") private val daysInAdvance: Long? = null
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    val day = LocalDate.now().plusDays(daysInAdvance!!)

    log.info("Scheduling all activities on $day")

    activityRepository.getAllForDate(day).parallelStream().forEach { activity ->
      activity
        .getSchedulesOnDay(day, includeSuspended = false)
        .filterActivitySchedulesWithNoPreExistingInstance(day)
        .forEach { schedule ->
          log.info("Scheduling instance of ${activity.summary} at ${activity.prisonCode} on $day")
          schedule.instances.add(
            ScheduledInstance(
              activitySchedule = schedule,
              sessionDate = day,
            )
          )
        }
      activityRepository.save(activity)
    }
  }

  private fun List<ActivitySchedule>.filterActivitySchedulesWithNoPreExistingInstance(day: LocalDate) =
    this.filter { it.instances.none { scheduledInstance -> scheduledInstance.sessionDate == day } }
}
