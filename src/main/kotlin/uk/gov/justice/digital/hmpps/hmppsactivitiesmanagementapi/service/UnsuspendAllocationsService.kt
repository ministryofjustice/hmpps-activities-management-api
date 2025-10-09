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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

@Service
class UnsuspendAllocationsService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val transactionHandler: TransactionHandler,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  private val allocationRepository: AllocationRepository,
  outboundEventsService: OutboundEventsService,
  monitoringService: MonitoringService,
) : ManageAllocationsBase(monitoringService, outboundEventsService) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun unsuspendAllocationsDueToBeUnsuspended() {
    rolloutPrisonService.getRolloutPrisons().forEach { unsuspendAllocations(it.prisonCode) }
  }

  fun sendEvents(job: Job) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending allocations due to be unsuspended job events for ${rolloutPrisons.count()} prisons")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.END_SUSPENSIONS,
        messageAttributes = PrisonCodeJobEvent(prison.prisonCode),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String) {
    unsuspendAllocations(prisonCode)

    log.debug("Marking allocations due to to be unsuspended sub-task complete for $prisonCode")

    jobService.incrementCount(jobId)
  }

  private fun unsuspendAllocations(prisonCode: String) {
    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Supplied prison $prisonCode is not rolled out."
    }

    transactionHandler.newSpringTransaction {
      allocationRepository.findByPrisonCodePrisonerStatus(prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY))
        .filterNot { it.isCurrentlySuspended() }
        .unsuspend()
    }.let(::sendAllocationsAmendedEvents)
  }

  private fun List<Allocation>.unsuspend() = continueToRunOnFailure(
    block = {
      onEach { allocation ->
        run {
          allocation.reactivateSuspension()
          allocationRepository.saveAndFlush(allocation)
        }
      }.map(Allocation::allocationId)
    },
    failure = "An error occurred while unsuspending allocations due to be unsuspended today",
  )
}
