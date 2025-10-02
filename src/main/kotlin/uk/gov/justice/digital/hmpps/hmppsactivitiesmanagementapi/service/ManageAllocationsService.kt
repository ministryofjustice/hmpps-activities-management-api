package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ManageAllocationsService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val allocationRepository: AllocationRepository,
  private val prisonerSearch: PrisonerSearchApiApplicationClient,
  private val transactionHandler: TransactionHandler,
  outboundEventsService: OutboundEventsService,
  monitoringService: MonitoringService,
  private val prisonerSearchApiApplicationClient: PrisonerSearchApiApplicationClient,
  private val prisonerReceivedHandler: PrisonerReceivedHandler,
) : ManageAllocationsBase(monitoringService, outboundEventsService) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun allocations() {
    log.info("Processing allocations starting on or before today.")
    processAllocationsDueToStartOnOrBeforeToday()
  }

  fun suspendAllocationsDueToBeSuspended(prisonCode: String) {
    transactionHandler.newSpringTransaction {
      allocationRepository.findByPrisonCodePrisonerStatus(prisonCode, listOf(PrisonerStatus.ACTIVE))
        .filter { it.isCurrentlySuspended() }
        .suspend()
    }.let(::sendAllocationsAmendedEvents)
  }

  fun unsuspendAllocationsDueToBeUnsuspended(prisonCode: String) {
    transactionHandler.newSpringTransaction {
      allocationRepository.findByPrisonCodePrisonerStatus(prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY))
        .filterNot { it.isCurrentlySuspended() }
        .unsuspend()
    }.let(::sendAllocationsAmendedEvents)
  }

  fun fixPrisonersIncorrectlyAutoSuspended() {
    allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)
      .groupBy { it.prisonerNumber }
      .forEach { (prisonerNumber, allocations) ->
        prisonerSearchApiApplicationClient.findByPrisonerNumber(prisonerNumber)?.also { prisoner ->

          prisoner.let { prisoner ->
            allocations
              .map { it.prisonCode() }
              .firstOrNull { prisonCode -> prisoner.isActiveInPrison(prisonCode) }
              ?.let { prisonCode ->
                log.info("Fixing stuck auto-suspended allocation(s) for prisoner $prisonerNumber in prison $prisonCode")

                prisonerReceivedHandler.receivePrisoner(prisonCode, prisonerNumber)
              }
          }
        } ?: run {
          log.warn("Unable to find prisoner $prisonerNumber to fix stuck auto-suspended allocation(s).")
        }
      }
  }

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

  private fun pendingAllocationsStartingOnOrBefore(date: LocalDate, prisonCode: String) = allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
    prisonCode,
    PrisonerStatus.PENDING,
    date,
  )

  private fun forEachRolledOutPrison() = rolloutPrisonService.getRolloutPrisons()

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
