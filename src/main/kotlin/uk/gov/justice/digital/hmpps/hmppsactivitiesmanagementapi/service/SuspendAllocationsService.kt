package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.SafeJobRunner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

@Service
class SuspendAllocationsService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val transactionHandler: TransactionHandler,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  private val allocationRepository: AllocationRepository,
  private val jobRunner: SafeJobRunner,
  private val unsuspendAllocationsService: UnsuspendAllocationsService,
  outboundEventsService: OutboundEventsService,
  monitoringService: MonitoringService,
) : ManageAllocationsBase(monitoringService, outboundEventsService) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun suspendAllocationsDueToBeSuspended() {
    rolloutPrisonService.getRolloutPrisons().forEach { suspendAllocations(it.prisonCode) }
  }

  fun sendEvents(job: Job) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending allocations due to be suspended job events for ${rolloutPrisons.count()} prisons")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.START_SUSPENSIONS,
        messageAttributes = PrisonCodeJobEvent(prison.prisonCode),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String) {
    suspendAllocations(prisonCode)

    log.debug("Marking allocations due to be suspended sub-task complete for $prisonCode")

    if (jobService.incrementCount(jobId)) {
      jobRunner.runDistributedJob(JobType.END_SUSPENSIONS, unsuspendAllocationsService::sendEvents)
    }
  }

  private fun suspendAllocations(prisonCode: String) {
    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Supplied prison $prisonCode is not rolled out."
    }

    transactionHandler.newSpringTransaction {
      allocationRepository.findByPrisonCodePrisonerStatus(prisonCode, listOf(PrisonerStatus.ACTIVE))
        .filter { it.isCurrentlySuspended() }
        .suspend()
    }.let(::sendAllocationsAmendedEvents)
  }

  private fun List<Allocation>.suspend() = continueToRunOnFailure(
    block = {
      onEach { allocation ->
        run {
          allocation.activatePlannedSuspension()
          allocationRepository.saveAndFlush(allocation)
        }
      }.map(Allocation::allocationId)
    },
    failure = "An error occurred while suspending allocations due to be suspended today",
  )
}
