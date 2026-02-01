package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isAtDifferentLocationTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isOutOfPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService.Companion.hasExpired

@Service
class ManageAllocationsDueToExpireService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val waitingListService: WaitingListService,
  private val transactionHandler: TransactionHandler,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val jobsSqsService: JobsSqsService,
  private val jobService: JobService,
  private val allocationRepository: AllocationRepository,
  outboundEventsService: OutboundEventsService,
  monitoringService: MonitoringService,
) : ManageAllocationsBase(monitoringService, outboundEventsService) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Caution to be used when using the current date. Allocations should be ended at the end of the day.
   */
  fun deallocateAllocationsDueToExpire() {
    rolloutPrisonService.getRolloutPrisons().forEach { deallocateAllocationsDueToExpireForPrison(it.prisonCode) }
  }

  fun sendAllocationsDueToExpireEvents(job: Job) {
    val rolloutPrisons = rolloutPrisonService.getRolloutPrisons()

    log.info("Sending allocations due to expire job events for ${rolloutPrisons.count()} prisons")

    jobService.initialiseCounts(job.jobId, rolloutPrisons.count())

    rolloutPrisons.forEach { prison ->
      val event = JobEventMessage(
        jobId = job.jobId,
        eventType = JobType.DEALLOCATE_EXPIRING,
        messageAttributes = PrisonCodeJobEvent(prison.prisonCode),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobId: Long, prisonCode: String) {
    deallocateAllocationsDueToExpireForPrison(prisonCode)

    log.debug("Marking allocations due to expire sub-task complete for $prisonCode")

    jobService.incrementCount(jobId)
  }

  private fun deallocateAllocationsDueToExpireForPrison(prisonCode: String) {
    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Supplied prison $prisonCode is not rolled out."
    }

    log.info("Checking for expired allocations at $prisonCode.")

    val prison = rolloutPrisonService.getByPrisonCode(prisonCode)

    deallocateIfExpired(prison, PrisonerStatus.PENDING)

    deallocateIfExpired(prison, PrisonerStatus.AUTO_SUSPENDED)

    waitingListService.fetchOpenApplicationsForPrison(prison.prisonCode).ifNotEmpty {
      log.info("Checking for expired waiting list applications at $prisonCode.")
      removeWaitingListApplicationIfExpired(it, prison)
    }
  }

  /*
   * The prison regime tells us how many days an allocation can stay before it expires. Given the max days from
   * the regime and the date of the last known movement out or prison for the prisoner any allocations should be
   * expired/deallocated.
   */
  private fun deallocateIfExpired(prison: RolloutPrisonPlan, prisonerStatus: PrisonerStatus) {
    log.info("Checking for expired $prisonerStatus allocations at ${prison.prisonCode}.")

    transactionHandler.newSpringTransaction {
      val allocations: List<Allocation> =
        allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(prisonerStatus))

      allocations.ifNotEmpty {
        log.info("Candidate allocations for expiration for prison ${prison.prisonCode}: ${allocations.map { it.allocationId }}")

        val prisonerNumbers = allocations.map { it.prisonerNumber }.distinct()
        val expiredPrisoners = getExpiredPrisoners(prison, prisonerNumbers.toSet())

        val expiredAllocations = allocations.filter { expiredPrisoners.contains(it.prisonerNumber) }
        log.info("Expired allocations for prison ${prison.prisonCode}: ${expiredAllocations.map { it.allocationId }}")

        expiredAllocations
          .groupBy { it.activitySchedule }
          .deallocate(DeallocationReason.TEMPORARILY_RELEASED)
      } ?: emptyList()
    }.let(::sendAllocationsAmendedEvents)
  }

  private fun getExpiredPrisoners(prison: RolloutPrisonPlan, prisonerNumbers: Set<String>): Set<String> {
    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers.toList())

    if (prisoners.isEmpty()) {
      log.error("No matches for prisoner numbers $prisonerNumbers found via prisoner search for prison ${prison.prisonCode}")
      return emptySet()
    }

    val prisonersNotInExpectedPrison =
      prisoners.filter { prisoner -> prisoner.isOutOfPrison() || prisoner.isAtDifferentLocationTo(prison.prisonCode) }

    return prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, prisonersNotInExpectedPrison.map { it.prisonerNumber }.toSet())
      .groupBy { it.offenderNo }
      .mapValues { it -> it.value.maxBy { it.movementDateTime() } }
      .filter { prison.hasExpired { it.value.movementDate } }
      .map { it.key }
      .toSet()
  }

  private fun Map<ActivitySchedule, List<Allocation>>.deallocate(reason: DeallocationReason): List<Long> = this.keys.map { schedule ->
    continueToRunOnFailure(
      block = {
        getOrDefault(schedule, emptyList())
          .onEach { allocation -> allocation.deallocateNowWithReason(reason) }
          .ifNotEmpty { deallocations ->
            activityScheduleRepository.saveAndFlush(schedule)
            log.info("Deallocated ${deallocations.size} allocation(s) from schedule ${schedule.activityScheduleId} with reason '$reason'.")
            deallocations
          }?.map(Allocation::allocationId) ?: emptyList()
      },
      failure = "An error occurred deallocating allocations on activity schedule ${schedule.activityScheduleId}",
    )
  }
    .flatMap { it }

  private fun removeWaitingListApplicationIfExpired(applications: Collection<WaitingList>, prisonPlan: RolloutPrisonPlan) {
    log.info("Waiting list applications for expiration for prison ${prisonPlan.prisonCode}: ${applications.map { it.waitingListId }}")

    val prisonerNumbers = applications.map { it.prisonerNumber }.distinct()
    val expiredPrisoners = getExpiredPrisoners(prisonPlan, prisonerNumbers.toSet())
    expiredPrisoners.forEach {
      waitingListService.removeOpenApplications(prisonPlan.prisonCode, it, ServiceName.SERVICE_NAME.value)
    }
  }
}
