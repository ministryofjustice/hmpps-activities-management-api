package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedFromCustodialSentence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedFromRemand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedOnDeath
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import java.time.LocalDateTime

@Component
class OffenderReleasedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonApiClient: PrisonApiApplicationClient,
) : EventHandler<OffenderReleasedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReleasedEvent): Boolean {
    log.info("Handling offender released event $event")

    if (rolloutPrisonRepository.findByCode(event.prisonCode())?.isActivitiesRolledOut() == true) {
      return when {
        event.isTemporary() -> {
          suspendOffenderAllocations(event)
          true
        }

        event.isPermanent() -> {
          deallocateOffenderAllocations(event)
          true
        }

        else -> {
          log.warn("Failed to handle event $event")
          false
        }
      }
    } else {
      log.info("Ignoring released event for ${event.prisonCode()} - not rolled out.")
    }

    return true
  }

  private fun suspendOffenderAllocations(event: OffenderReleasedEvent) =
    allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .suspendAndSaveAffectedAllocations()
      .let {
        log.info("Suspended ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.")
      }

  private fun deallocateOffenderAllocations(event: OffenderReleasedEvent) =
    prisonApiClient.getPrisonerDetails(
      prisonerNumber = event.prisonerNumber(),
      fullInfo = true,
      extraInfo = true,
    ).block()?.let { prisoner ->
      when {
        prisoner.isReleasedOnDeath() -> "Dead"
        prisoner.isReleasedFromRemand() -> "Released"
        prisoner.isReleasedFromCustodialSentence() -> "Released"
        else -> log.warn("Unable to determine release reason for prisoner ${event.prisonerNumber()}").let { null }
      }
    }?.let { reason ->
      allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
        .deallocateAndSaveAffectedAllocations(reason)
        .also {
          log.info("Deallocated prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()} from ${it.size} allocations.")
        }
      true
    } ?: log.warn("Prisoner for $event not found").let { false }

  private fun List<Allocation>.suspendAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filter { it.status(PrisonerStatus.ACTIVE) }.map { it.autoSuspend(now, "Temporarily released from prison") }
    }.saveAffectedAllocations()

  private fun List<Allocation>.deallocateAndSaveAffectedAllocations(reason: String = "Released from prison") =
    LocalDateTime.now().let { now ->
      this.filterNot { it.status(PrisonerStatus.ENDED) }.map { it.deallocate(now, reason) }
    }.saveAffectedAllocations()

  private fun List<Allocation>.saveAffectedAllocations() =
    allocationRepository.saveAllAndFlush(this).toList()
}
