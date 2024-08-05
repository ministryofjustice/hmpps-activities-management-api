package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.Clock
import java.time.LocalDate

@Service
class ManageScheduledInstancesService(
  private val activityRepository: ActivityRepository,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val transactionHandler: CreateInstanceTransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  private val monitoringService: MonitoringService,
  @Value("\${jobs.create-scheduled-instances.days-in-advance}") private val daysInAdvance: Long? = 0L,
  private val clock: Clock,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(propagation = Propagation.REQUIRED)
  fun create() {
    val today = LocalDate.now(clock)
    val endDay = today.plusDays(daysInAdvance!!)
    val listOfDatesToSchedule = today.datesUntil(endDay).toList()
    log.info("Scheduling activities job running - from $today until $endDay")

    rolloutPrisonRepository.findAll()
      .filter { it.isActivitiesRolledOut() }
      .forEach { prison ->
        log.info("Scheduling activities for prison ${prison.description} until $endDay")
        val activities = activityRepository.getBasicForPrisonBetweenDates(prison.code, today, endDay)
        activities.mapNotNull { basic ->
          continueToRunOnFailure(
            block = { transactionHandler.createInstancesForActivitySchedule(prison, basic.activityScheduleId, listOfDatesToSchedule) },
            success = "Scheduled instances for ${prison.code} ${basic.summary}",
            failure = "Failed to schedule instances for ${prison.code} ${basic.summary}",
          )
        }.forEach { updatedScheduledId ->
          outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, updatedScheduledId)
        }
      }
  }

  private fun <R> continueToRunOnFailure(block: () -> R?, success: String, failure: String) =
    runCatching { block() }.onSuccess { log.info(success) }.onFailure {
      monitoringService.capture(failure, it)
      log.error(failure, it)
    }.getOrNull()
}

/**
 * The instances for each activity schedule need to be created within its own transaction.
 * This is because sync events are generated on the saveAndFlush, and where this is
 * part of a longer job, if the entire job were a single transaction the sync service would
 * see the earlier events BEFORE the data is committed - and find nothing when it calls back
 * in response to the `activities.scheduled-instances.created` event.
 *
 * It also needs to be a separate bean so that the Spring @Transactional annotations are honoured - they
 * work through bean proxies, and if called within the same bean the transactional annotations have no effect.
 */
@Component
class CreateInstanceTransactionHandler(
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val activityService: ActivityService,
  private val clock: Clock,
) {
  /**
   * Returns the schedule ID if the schedule is updated otherwise returns null.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun createInstancesForActivitySchedule(prison: RolloutPrison, scheduleId: Long, days: List<LocalDate>): Long? {
    val earliestSession = LocalDate.now(clock)
    val schedule = activityScheduleRepository.getActivityScheduleByIdWithFilters(scheduleId, earliestSession)
      ?: throw EntityNotFoundException("Activity schedule ID $scheduleId not found")

    val lastUpdate = schedule.instancesLastUpdatedTime
    activityService.addScheduleInstances(schedule, days)
    if (schedule.instancesLastUpdatedTime != lastUpdate) {
      activityScheduleRepository.saveAndFlush(schedule)

      return scheduleId
    }

    return null
  }
}
