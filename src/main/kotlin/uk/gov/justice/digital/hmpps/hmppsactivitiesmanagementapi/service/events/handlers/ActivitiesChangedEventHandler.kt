package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ActivitiesChangedEvent
import java.time.LocalDateTime

@Component
class ActivitiesChangedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
) : EventHandler<ActivitiesChangedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: ActivitiesChangedEvent): Outcome {
    log.info("Handling activities changed event $event")

    if (rolloutPrisonRepository.findByCode(event.prisonCode())?.isActivitiesRolledOut() == true) {
      return when (event.action()) {
        Action.SUSPEND -> suspendOffenderAllocations(event).let { Outcome.success() }
        Action.END -> deallocateOffenderAllocations(event).let { Outcome.success() }
        else -> log.warn("Unable to process $event, unknown action").let { Outcome.failed() }
      }
    }

    return Outcome.success()
  }

  private fun suspendOffenderAllocations(event: ActivitiesChangedEvent) =
    allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .suspendAndSaveAffectedAllocations()
      .let {
        log.info("Suspended ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.")
      }

  private fun List<Allocation>.suspendAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filter { it.status(PrisonerStatus.ACTIVE) }.map { it.autoSuspend(now, "Temporary absence") }
    }.saveAffectedAllocations()

  private fun deallocateOffenderAllocations(event: ActivitiesChangedEvent) =
    allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .deallocateAndSaveAffectedAllocations(DeallocationReason.TEMPORARY_ABSENCE)
      .also {
        log.info("Deallocated prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()} from ${it.size} allocations.")
      }

  private fun List<Allocation>.deallocateAndSaveAffectedAllocations(reason: DeallocationReason) =
    this.filterNot { it.status(PrisonerStatus.ENDED) }.map { it.deallocateNow(reason) }.saveAffectedAllocations()

  private fun List<Allocation>.saveAffectedAllocations() =
    allocationRepository.saveAllAndFlush(this).toList()
}
