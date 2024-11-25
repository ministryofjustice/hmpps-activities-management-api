package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isAtDifferentLocationTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isOutOfPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService.Companion.hasExpired
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class ManageAllocationsService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
  private val waitingListService: WaitingListService,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  private val prisonApiClient: PrisonApiClient,
  private val monitoringService: MonitoringService,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun allocations(operation: AllocationOperation) {
    when (operation) {
      AllocationOperation.STARTING_TODAY -> {
        log.info("Processing allocations starting on or before today.")
        processAllocationsDueToStartOnOrBeforeToday()
      }

      AllocationOperation.EXPIRING_TODAY -> {
        log.info("Expiring allocations due to expire today.")
        deallocateAllocationsDueToExpire()
      }
    }
  }

  fun suspendAllocationsDueToBeSuspended(prisonCode: String) {
    allocationRepository.findByPrisonCodePrisonerStatus(prisonCode, PrisonerStatus.ACTIVE)
      .filter { it.isCurrentlySuspended() }
      .suspend()
  }

  fun unsuspendAllocationsDueToBeUnsuspended(prisonCode: String) {
    allocationRepository.findByPrisonCodePrisonerStatus(prisonCode, PrisonerStatus.SUSPENDED)
      .filterNot { it.isCurrentlySuspended() }
      .unsuspend()
  }

  /**
   * Caution to be used when using the current date. Allocations should be ended at the end of the day.
   */
  fun endAllocationsDueToEnd(prisonCode: String, date: LocalDate) {
    require(rolloutPrisonService.isActivitiesRolledOutAt(prisonCode)) {
      "Supplied prison $prisonCode is not rolled out."
    }

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

  private fun declineWaitingListsFor(schedule: ActivitySchedule) {
    waitingListService.declinePendingOrApprovedApplications(
      schedule.activity.activityId,
      "Activity ended",
      ServiceName.SERVICE_NAME.value,
    )
  }

  private fun ActivitySchedule.deallocateAllocationsForScheduleEndingOn(date: LocalDate) =
    allocations(excludeEnded = true).onEach { allocation -> allocation.deallocateNowOn(date) }.map(Allocation::allocationId)

  private fun ActivitySchedule.deallocateAllocationsEndingOn(date: LocalDate) =
    allocations(true)
      .filter { activeAllocation -> activeAllocation.endsOn(date) }
      .onEach { allocation -> allocation.deallocateNowOn(date) }
      .map(Allocation::allocationId)

  /*
   * We can consider pending allocations before today in the event we need to (re)run due to something out of our control
   * e.g. a job fails to run due to a cloud platform issue.
   */
  private fun processAllocationsDueToStartOnOrBeforeToday() {
    LocalDate.now().let { today ->
      forEachRolledOutPrison()
        .forEach { prison ->
          transactionHandler.newSpringTransaction {
            pendingAllocationsStartingOnOrBefore(today, prison.prisonCode).let { allocations ->
              val prisoners = prisonerSearch.findByPrisonerNumbers(allocations.map { it.prisonerNumber }.distinct())

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
                log.info("Activated ${it.filter { a -> a.status(PrisonerStatus.ACTIVE) }.size} pending allocation(s) at prison ${prison.prisonCode}.")
                log.info("Auto-Suspended ${it.filter { a -> a.status(PrisonerStatus.AUTO_SUSPENDED) }.size} pending allocation(s) at prison ${prison.prisonCode}.")
              }.map(Allocation::allocationId)
          }.let(::sendAllocationsAmendedEvents)
        }
    }
  }

  private fun pendingAllocationsStartingOnOrBefore(date: LocalDate, prisonCode: String) =
    allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
      prisonCode,
      PrisonerStatus.PENDING,
      date,
    )

  private fun forEachRolledOutPrison() =
    rolloutPrisonService.getRolloutPrisons()

  private fun deallocateAllocationsDueToExpire() {
    forEachRolledOutPrison()
      .forEach { prison ->
        log.info("Checking for expired allocations at ${prison.prisonCode}.")

        allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, PrisonerStatus.PENDING).ifNotEmpty {
          log.info("Checking for expired pending allocations at ${prison.prisonCode}.")
          deallocateIfExpired(it, prison)
        }
        allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, PrisonerStatus.AUTO_SUSPENDED).ifNotEmpty {
          log.info("Checking for expired auto-suspended allocations at ${prison.prisonCode}.")
          deallocateIfExpired(it, prison)
        }
        waitingListService.fetchOpenApplicationsForPrison(prison.prisonCode).ifNotEmpty {
          log.info("Checking for expired waiting list applications at ${prison.prisonCode}.")
          removeWaitingListApplicationIfExpired(it, prison)
        }
      }
  }

  /*
   * The prison regime tells us how many days an allocation can stay before it expires. Given the max days from
   * the regime and the date of the last known movement out or prison for the prisoner any allocations should be
   * expired/deallocated.
   */
  private fun deallocateIfExpired(allocations: Collection<Allocation>, prisonPlan: RolloutPrisonPlan) {
    log.info("Candidate allocations for expiration for prison ${prisonPlan.prisonCode}: ${allocations.map { it.allocationId }}")

    val prisonerNumbers = allocations.map { it.prisonerNumber }.distinct()
    val expiredPrisoners = getExpiredPrisoners(prisonPlan, prisonerNumbers.toSet())

    val expiredAllocations = allocations.filter { expiredPrisoners.contains(it.prisonerNumber) }
    log.info("Expired allocations for prison ${prisonPlan.prisonCode}: ${expiredAllocations.map { it.allocationId }}")

    expiredAllocations
      .groupBy { it.activitySchedule }
      .deallocate(DeallocationReason.TEMPORARILY_RELEASED)
  }

  private fun removeWaitingListApplicationIfExpired(applications: Collection<WaitingList>, prisonPlan: RolloutPrisonPlan) {
    log.info("Waiting list applications for expiration for prison ${prisonPlan.prisonCode}: ${applications.map { it.waitingListId }}")

    val prisonerNumbers = applications.map { it.prisonerNumber }.distinct()
    val expiredPrisoners = getExpiredPrisoners(prisonPlan, prisonerNumbers.toSet())
    expiredPrisoners.forEach {
      waitingListService.removeOpenApplications(prisonPlan.prisonCode, it, ServiceName.SERVICE_NAME.value)
    }
  }

  fun getExpiredPrisoners(prisonPlan: RolloutPrisonPlan, prisonerNumbers: Set<String>): Set<String> {
    val prisoners = prisonerSearch.findByPrisonerNumbers(prisonerNumbers.toList())

    if (prisoners.isEmpty()) {
      log.error("No matches for prisoner numbers $prisonerNumbers found via prisoner search for prison ${prisonPlan.prisonCode}")
      return emptySet()
    }

    val prisonersNotInExpectedPrison =
      prisoners.filter { prisoner -> prisoner.isOutOfPrison() || prisoner.isAtDifferentLocationTo(prisonPlan.prisonCode) }

    return prisonApiClient.getMovementsForPrisonersFromPrison(prisonPlan.prisonCode, prisonersNotInExpectedPrison.map { it.prisonerNumber }.toSet())
      .groupBy { it.offenderNo }
      .mapValues { it -> it.value.maxBy { it.movementDateTime() } }
      .filter { prisonPlan.hasExpired { it.value.movementDate } }
      .map { it.key }
      .toSet()
  }

  private fun Map<ActivitySchedule, List<Allocation>>.deallocate(reason: DeallocationReason? = null) {
    this.keys.forEach { schedule ->
      continueToRunOnFailure(
        block = {
          transactionHandler.newSpringTransaction {
            getOrDefault(schedule, emptyList()).onEach { allocation ->
              reason?.let { allocation.deallocateNowWithReason(reason) } ?: allocation.deallocateNowOn(LocalDate.now())
            }.ifNotEmpty { deallocations ->
              activityScheduleRepository.saveAndFlush(schedule)
              log.info("Deallocated ${deallocations.size} allocation(s) from schedule ${schedule.activityScheduleId} with reason '$reason'.")
              deallocations
            }?.map(Allocation::allocationId) ?: emptyList()
          }.let(::sendAllocationsAmendedEvents)
        },
        failure = "An error occurred deallocating allocations on activity schedule ${schedule.activityScheduleId}",
      )
    }
  }

  private fun List<Allocation>.suspend() =
    continueToRunOnFailure(
      block = {
        transactionHandler.newSpringTransaction {
          onEach { allocation ->
            run {
              allocation.activatePlannedSuspension()
              allocationRepository.saveAndFlush(allocation)
            }
          }.map(Allocation::allocationId)
        }.let(::sendAllocationsAmendedEvents)
      },
      failure = "An error occurred while suspending allocations due to be suspended today",
    )

  private fun List<Allocation>.unsuspend() =
    continueToRunOnFailure(
      block = {
        transactionHandler.newSpringTransaction {
          onEach { allocation ->
            run {
              allocation.reactivateSuspension()
              allocationRepository.saveAndFlush(allocation)
            }
          }.map(Allocation::allocationId)
        }.let(::sendAllocationsAmendedEvents)
      },
      failure = "An error occurred while unsuspending allocations due to be unsuspended today",
    )

  private fun sendAllocationsAmendedEvents(allocationIds: Collection<Long>) {
    log.info("Sending allocation amended events for allocation IDs ${allocationIds.joinToString(separator = ",")}.")

    allocationIds.forEach { outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it) }
  }

  private fun continueToRunOnFailure(block: () -> Unit, failure: String = "") {
    runCatching {
      block()
    }
      .onFailure {
        monitoringService.capture(failure, it)
        log.error(failure, it)
      }
  }
}

enum class AllocationOperation {
  EXPIRING_TODAY,
  STARTING_TODAY,
}
