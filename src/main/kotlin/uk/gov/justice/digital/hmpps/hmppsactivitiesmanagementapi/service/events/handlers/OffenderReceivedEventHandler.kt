package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReceivedEvent

@Component
class OffenderReceivedEventHandler(private val repository: AllocationRepository) : EventHandler<OffenderReceivedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReceivedEvent): Boolean =
    log.info("Handling received event $event").run {
      repository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
        .reactivateAndSaveAffectedAllocations()
        .also { log.info("Reactivated ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.") }

      return true
    }

  private fun List<Allocation>.reactivateAndSaveAffectedAllocations() =
    this.filter { it.status(PrisonerStatus.AUTO_SUSPENDED) }
      .map { it.reactivateAutoSuspensions() }
      .saveAffectedAllocations()

  private fun List<Allocation>.saveAffectedAllocations() =
    repository.saveAllAndFlush(this).toList()
}
