package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import java.time.LocalDateTime

@Component
class InboundEventsProcessor(private val repository: AllocationRepository) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun process(inboundEvent: InboundEvent) {
    when (inboundEvent) {
      is OffenderReceivedEvent -> reactivateSuspendedOffenderAllocations(inboundEvent)

      is OffenderReleasedEvent -> {
        when {
          inboundEvent.isTemporary() -> suspendOffenderAllocations(inboundEvent)
          inboundEvent.isPermanent() -> deallocateOffenderAllocations(inboundEvent)
          // TODO pick up with the event of interest work.
          else -> log.info("Ignoring event of potential interest $inboundEvent.")
        }
      }

      else -> log.warn("Unsupported event ${inboundEvent.javaClass.name}")
    }
  }

  private fun suspendOffenderAllocations(inboundEvent: InboundEvent) {
    repository.findByPrisonCodeAndPrisonerNumber(inboundEvent.prisonCode(), inboundEvent.prisonerNumber())
      .suspendAndSaveAffectedAllocations()
      .also { log.info("Suspended ${it.size} allocations for prisoner ${inboundEvent.prisonerNumber()} at prison ${inboundEvent.prisonCode()}.") }
  }

  private fun deallocateOffenderAllocations(inboundEvent: InboundEvent) {
    repository.findByPrisonCodeAndPrisonerNumber(inboundEvent.prisonCode(), inboundEvent.prisonerNumber())
      .deallocateAndSaveAffectedAllocations()
      .also { log.info("Deallocated prisoner ${inboundEvent.prisonerNumber()} at prison ${inboundEvent.prisonCode()} from ${it.size} allocations.") }
  }

  private fun reactivateSuspendedOffenderAllocations(inboundEvent: InboundEvent) {
    repository.findByPrisonCodeAndPrisonerNumber(inboundEvent.prisonCode(), inboundEvent.prisonerNumber())
      .reactivateAndSaveAffectedAllocations()
      .also { log.info("Reactivated ${it.size} allocations for prisoner ${inboundEvent.prisonerNumber()} at prison ${inboundEvent.prisonCode()}.") }
  }

  private fun List<Allocation>.reactivateAndSaveAffectedAllocations() =
    this.filter { it.status(PrisonerStatus.AUTO_SUSPENDED) }
      .map { it.reactivateAutoSuspensions() }
      .saveAffectedAllocations()

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
