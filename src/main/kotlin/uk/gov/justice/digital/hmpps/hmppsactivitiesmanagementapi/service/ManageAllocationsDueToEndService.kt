package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

@Service
class ManageAllocationsDueToEndService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val waitingListService: WaitingListService,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  @Value("\${jobs.deallocate-allocations-ending.days-start}") private val deallocateDaysStart: Int = 3,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Caution to be used when using the current date. Allocations should be ended at the end of the day.
   */
  fun endAllocationsDueToEnd() {
    rolloutPrisonService.getRolloutPrisons().forEach { endAllocationsDueToEndForPrison(it.prisonCode) }
  }

  fun sendAllocationsDueToEndEvents(job: Job) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending allocations due to end job events for ${rolloutPrisons.count()} prisons")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.DEALLOCATE_ENDING,
        messageAttributes = PrisonCodeJobEvent(prison.prisonCode),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String) {
    endAllocationsDueToEndForPrison(prisonCode)

    log.debug("Marking allocations due to end sub-task complete for $prisonCode")

    jobService.incrementCount(jobId)
  }

  private fun endAllocationsDueToEndForPrison(prisonCode: String) {
    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Supplied prison $prisonCode is not rolled out."
    }

    val startDate = deallocateDaysStart.daysAgo()
    val endDate = 1.daysAgo()

    startDate.rangeTo(endDate).forEach { date ->
      transactionHandler.newSpringTransaction {
        activityScheduleRepository.findAllByActivityPrisonCode(prisonCode).flatMap { schedule ->
          if (schedule.endsOn(date)) {
            declineWaitingListsFor(schedule)
            schedule.deallocateAllocationsForScheduleEndingOn(date)
          } else {
            schedule.deallocateAllocationsEndingOn(date)
          }.also { allocationIds -> allocationIds.ifNotEmpty { activityScheduleRepository.saveAndFlush(schedule) } }
        }
      }.let(::sendAllocationsAmendedEvents)
    }
  }

  private fun declineWaitingListsFor(schedule: ActivitySchedule) {
    waitingListService.declinePendingOrApprovedApplications(
      schedule.activity.activityId,
      "Activity ended",
      ServiceName.SERVICE_NAME.value,
    )
  }

  private fun ActivitySchedule.deallocateAllocationsForScheduleEndingOn(date: LocalDate) = allocations(excludeEnded = true).onEach { allocation -> allocation.deallocateNowOn(date) }.map(Allocation::allocationId)

  private fun ActivitySchedule.deallocateAllocationsEndingOn(date: LocalDate) = allocations(true)
    .filter { activeAllocation -> activeAllocation.endsOn(date) }
    .onEach { allocation -> allocation.deallocateNowOn(date) }
    .map(Allocation::allocationId)

  private fun sendAllocationsAmendedEvents(allocationIds: Collection<Long>) {
    log.info("Sending allocation amended events for allocation IDs ${allocationIds.joinToString(separator = ",")}.")

    allocationIds.forEach { outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it) }
  }
}
