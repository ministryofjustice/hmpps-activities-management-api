package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OffenderDeallocationService(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun deallocateOffendersWhenEndDatesReached() {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()

    rolloutPrisonRepository.findAllByActiveIsTrue().forEach { prison ->
      activityRepository.getAllForPrisonAndDate(prison.code, today).forEach { activity ->
        if (activity.ends(today)) {
          activity.schedules().deallocateAllOffenders(now)
        } else {
          activity.schedules().deallocateOffendersEnding(today, now)
        }
      }
    }
  }

  private fun List<ActivitySchedule>.deallocateOffendersEnding(date: LocalDate, timestamp: LocalDateTime) {
    this.associateSchedulesWithAllocationsEnding(date).forEach { (schedule, allocations) ->
      continueToRunOnFailure(
        block = {
          allocations?.let {
            allocations.deallocate(timestamp)
            activityScheduleRepository.saveAndFlush(schedule)
          }
        },
      )
    }
  }

  private fun List<ActivitySchedule>.deallocateAllOffenders(dateTime: LocalDateTime) {
    this.forEach { schedule ->
      continueToRunOnFailure(
        block = {
          schedule.allocations().deallocate(dateTime)
          activityScheduleRepository.saveAndFlush(schedule)
        },
      )
    }
  }

  private fun continueToRunOnFailure(block: () -> Unit, failure: String = "") {
    runCatching {
      block()
    }
      .onFailure { log.error(failure, it) }
  }

  fun List<Allocation>.deallocate(dateTime: LocalDateTime) {
    this.forEach { it.deallocate(dateTime, "Allocation end date reached") }
  }

  private fun List<ActivitySchedule>.associateSchedulesWithAllocationsEnding(date: LocalDate) =
    this.associateWith { it.allocations().ending(date) }

  private fun List<Allocation>.ending(date: LocalDate) =
    filter { it.ends(date) && !it.status(PrisonerStatus.ENDED) }.ifEmpty { null }
}
