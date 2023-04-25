package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import java.time.LocalDateTime

@Component
class OffenderReleasedEventHandler(private val repository: AllocationRepository) : EventHandler<OffenderReleasedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // TODO Needs to use the prison-api-client to check the state in NOMIS.
  // TODO any issues then flag as event of interest.
  override fun handle(event: OffenderReleasedEvent) {
    when {
      event.isTemporary() -> suspendOffenderAllocations(event)
      event.isPermanent() -> deallocateOffenderAllocations(event)
      // TODO pick up with the event of interest work.
      else -> log.info("Ignoring event of potential interest $event.")
    }
  }

  private fun suspendOffenderAllocations(event: OffenderReleasedEvent) {
    repository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .suspendAndSaveAffectedAllocations()
      .also { log.info("Suspended ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.") }
  }

  private fun deallocateOffenderAllocations(event: OffenderReleasedEvent) {
    repository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .deallocateAndSaveAffectedAllocations()
      .also { log.info("Deallocated prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()} from ${it.size} allocations.") }
  }

  private fun List<Allocation>.suspendAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filter { it.status(PrisonerStatus.ACTIVE) }.map { it.autoSuspend(now, "Temporarily released from prison") }
    }.saveAffectedAllocations()

  private fun List<Allocation>.deallocateAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filterNot { it.status(PrisonerStatus.ENDED) }.map { it.deallocate(now, "Released from prison") }
    }.saveAffectedAllocations()

  private fun List<Allocation>.saveAffectedAllocations() =
    repository.saveAllAndFlush(this).toList()
}
