package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
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
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageNewAllocationsService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val transactionHandler: TransactionHandler,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  private val allocationRepository: AllocationRepository,
  private val jobRunner: SafeJobRunner,
  private val suspendAllocationsService: SuspendAllocationsService,
  outboundEventsService: OutboundEventsService,
  monitoringService: MonitoringService,
) : ManageAllocationsBase(monitoringService, outboundEventsService) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun allocations() {
    rolloutPrisonService.getRolloutPrisons().forEach { createNewAllocations(it.prisonCode) }
  }

  fun sendAllocationEvents(job: Job) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending allocations due to start job events for ${rolloutPrisons.count()} prisons")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.ALLOCATE,
        messageAttributes = PrisonCodeJobEvent(prison.prisonCode),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String) {
    createNewAllocations(prisonCode)

    log.debug("Marking allocations due to start sub-task complete for $prisonCode")

    if (jobService.incrementCount(jobId)) {
      jobRunner.runDistributedJob(JobType.START_SUSPENSIONS, suspendAllocationsService::sendEvents)
    }
  }

  /*
   * We can consider pending allocations before today in the event we need to (re)run due to something out of our control
   * e.g. a job fails to run due to a cloud platform issue.
   */
  private fun createNewAllocations(prisonCode: String) {
    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Supplied prison $prisonCode is not rolled out."
    }

    val today = LocalDate.now()

    transactionHandler.newSpringTransaction {
      pendingAllocationsStartingOnOrBefore(today, prisonCode).let { allocations ->
        val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(allocations.map { it.prisonerNumber }.distinct())

        allocations.map { allocation -> allocation to prisoners.firstOrNull { it.prisonerNumber == allocation.prisonerNumber } }
      }
        .onEach { (allocation, prisoner) ->
          prisoner?.let {
            if (prisoner.isActiveInPrison(allocation.prisonCode())) {
              allocation.activate()
            } else {
              allocation.autoSuspend(LocalDateTime.now(), "Temporarily released or transferred")
            }

            allocationRepository.saveAndFlush(allocation)
          }
            ?: log.error("Unable to process pending allocation ${allocation.allocationId}, prisoner ${allocation.prisonerNumber} not found.")
        }
        .map { (allocation, _) -> allocation }
        .also {
          log.info("Activated ${it.filter { a -> a.status(PrisonerStatus.ACTIVE) }.size} pending allocation(s) at prison $prisonCode.")
          log.info("Auto-Suspended ${it.filter { a -> a.status(PrisonerStatus.AUTO_SUSPENDED) }.size} pending allocation(s) at prison $prisonCode.")
        }.map(Allocation::allocationId)
    }.let(::sendAllocationsAmendedEvents)
  }

  private fun pendingAllocationsStartingOnOrBefore(date: LocalDate, prisonCode: String) = allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
    prisonCode,
    PrisonerStatus.PENDING,
    date,
  )
}
