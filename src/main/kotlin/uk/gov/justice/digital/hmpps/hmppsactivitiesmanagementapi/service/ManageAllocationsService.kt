package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isOutOfPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.lastMovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageAllocationsService(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonRegimeRepository: PrisonRegimeRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
  private val waitingListService: WaitingListService,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
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

      AllocationOperation.ENDING_TODAY -> {
        log.info("Ending allocations due to end today.")
        allocationsDueToEnd().deallocate()
      }

      AllocationOperation.EXPIRING_TODAY -> {
        log.info("Expiring allocations due to expire today.")
        allocationsDueToExpire().deallocate(DeallocationReason.EXPIRED)
      }
    }
  }

  /**
   * We consider pending allocations before today in the event we need to (re)run due to something out of our control
   * e.g. a job fails to run due to a cloud platform issue.
   */
  private fun processAllocationsDueToStartOnOrBeforeToday() {
    LocalDate.now().let { today ->
      forEachRolledOutPrison()
        .forEach { prison ->
          transactionHandler.newSpringTransaction {
            allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
              prison.code,
              PrisonerStatus.PENDING,
              today,
            )
              .onEach { allocation ->
                prisonerSearch.findByPrisonerNumber(allocation.prisonerNumber)?.let { prisoner ->
                  if (prisoner.isOutOfPrison()) {
                    allocation.autoSuspend(LocalDateTime.now(), "Temporarily released or transferred")
                  } else {
                    allocation.activate()
                  }

                  allocationRepository.saveAndFlush(allocation)
                }
                  ?: log.error("Unable to process pending allocation ${allocation.allocationId}, prisoner ${allocation.prisonerNumber} not found.")
              }.also {
                log.info("Activated ${it.filter { a -> a.status(PrisonerStatus.ACTIVE) }.size} pending allocation(s) at prison ${prison.code}.")
                log.info("Suspended ${it.filter { a -> a.status(PrisonerStatus.AUTO_SUSPENDED) }.size} pending allocation(s) at prison ${prison.code}.")
              }.map(Allocation::allocationId)
          }.let(::sendAllocationsAmendedEvents)
        }
    }
  }

  private fun List<ActivitySchedule>.allocationsDueToStartOnOrBefore(date: LocalDate) =
    flatMap { it.allocations().filter { allocation -> allocation.startDate <= date } }

  private fun allocationsDueToEnd(): Map<ActivitySchedule, List<Allocation>> =
    LocalDate.now().let { today ->
      forEachRolledOutPrison()
        .flatMap { prison ->
          activityRepository.getAllForPrisonAndDate(prison.code, today).flatMap { activity ->
            if (activity.ends(today)) {
              waitingListService.declinePendingOrApprovedApplications(
                activity.activityId,
                "Activity ended",
                ServiceName.SERVICE_NAME.value,
              )
              activity.schedules().flatMap { it.allocations().filterNot(Allocation::isEnded) }
            } else {
              activity.schedules().flatMap { it.allocations().ending(today) }
            }
          }
        }.groupBy { it.activitySchedule }
    }

  private fun forEachRolledOutPrison() =
    rolloutPrisonRepository.findAll().filter { it.isActivitiesRolledOut() }

  private fun List<Allocation>.ending(date: LocalDate) =
    filterNot { it.status(PrisonerStatus.ENDED) }.filter { it.ends(date) }

  private fun allocationsDueToExpire(): Map<ActivitySchedule, List<Allocation>> =
    forEachRolledOutPrison()
      .flatMap { prison ->
        val regime = prisonRegimeRepository.findByPrisonCode(prison.code)

        if (regime != null) {
          val candidateExpiredAllocations =
            allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.AUTO_SUSPENDED)
              .filter(regime::hasExpired)

          val expiredPrisoners = candidateExpiredAllocations.ifNotEmpty {
            prisonerSearch.findByPrisonerNumbers(candidateExpiredAllocations.map { it.prisonerNumber }.distinct())
              .block()
              ?.filter { it.hasExpired(regime) }
              ?.map { it.prisonerNumber }
              ?.toSet()
              ?.onEach {
                waitingListService.declinePendingOrApprovedApplications(
                  prison.code,
                  it,
                  "Released",
                  ServiceName.SERVICE_NAME.value,
                )
              }
              ?: emptySet()
          }

          candidateExpiredAllocations.filter { expiredPrisoners.contains(it.prisonerNumber) }
        } else {
          log.warn("Rolled out prison ${prison.code} is missing a prison regime.")
          emptyList()
        }
      }.groupBy { it.activitySchedule }

  private fun Prisoner.hasExpired(prisonRegime: PrisonRegime) =
    if (isOutOfPrison()) {
      when (lastMovementType()) {
        MovementType.RELEASE -> prisonRegime.hasExpired { this.releaseDate }
        MovementType.TEMPORARY_ABSENCE -> true
        MovementType.TRANSFER -> true
        else -> false
      }
    } else {
      false
    }

  private fun Map<ActivitySchedule, List<Allocation>>.deallocate(reason: DeallocationReason? = null) {
    this.keys.forEach { schedule ->
      continueToRunOnFailure(
        block = {
          transactionHandler.newSpringTransaction {
            getOrDefault(schedule, emptyList()).onEach { allocation ->
              reason?.let { allocation.deallocateNowWithReason(reason) } ?: allocation.deallocateNow()
            }.ifNotEmpty { deallocations ->
              activityScheduleRepository.saveAndFlush(schedule)
              log.info("Deallocated ${deallocations.size} allocation(s) from schedule ${schedule.activityScheduleId} with reason '$reason'.")
              deallocations
            }.map(Allocation::allocationId)
          }.let(::sendAllocationsAmendedEvents)
        },
        failure = "An error occurred deallocating allocations on activity schedule ${schedule.activityScheduleId}.",
      )
    }
  }

  private fun <T, R> Collection<T>.ifNotEmpty(block: (Collection<T>) -> Collection<R>) =
    if (this.isNotEmpty()) {
      block(this)
    } else {
      emptySet()
    }

  private fun sendAllocationsAmendedEvents(allocationIds: Collection<Long>) =
    allocationIds.forEach { outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it) }
      .also { log.info("Sending allocation amended events.") }

  private fun continueToRunOnFailure(block: () -> Unit, failure: String = "") {
    runCatching {
      block()
    }
      .onFailure { log.error(failure, it) }
  }
}

enum class AllocationOperation {
  ENDING_TODAY,
  EXPIRING_TODAY,
  STARTING_TODAY,
}
