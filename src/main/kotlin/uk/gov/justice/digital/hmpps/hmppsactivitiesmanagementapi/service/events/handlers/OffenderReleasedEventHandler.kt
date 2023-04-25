package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class OffenderReleasedEventHandler(
  private val prisonApiClient: PrisonApiApplicationClient,
  private val repository: AllocationRepository,
) : EventHandler<OffenderReleasedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReleasedEvent) {
    when {
      // TODO temporary release probably needs further validation checks e.g. check state of prisoner against prison-api
      event.isTemporary() -> suspendOffenderAllocations(event)
      event.isPermanent() -> deallocateOffenderAllocations(event)
      else -> flagEventOfInterest(event)
    }
  }

  private fun suspendOffenderAllocations(event: OffenderReleasedEvent) {
    repository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .suspendAndSaveAffectedAllocations()
      .also { log.info("Suspended ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.") }
  }

  private fun deallocateOffenderAllocations(event: OffenderReleasedEvent) {
    prisonApiClient.getPrisonerDetails(event.prisonerNumber()).block()?.let { prisoner ->
      when {
        prisoner.isReleasedOnDeath() -> "Dead"
        prisoner.isReleasedFromRemand() -> "Released"
        prisoner.isReleasedFromCustodialSentence() -> "Released"
        else -> null
      }
    }?.let { reason ->
      repository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
        .deallocateAndSaveAffectedAllocations(reason)
        .also { log.info("Deallocated prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()} from ${it.size} allocations.") }
    } ?: flagEventOfInterest(event)
  }

  private fun InmateDetail.isReleasedOnDeath(): Boolean = this.legalStatus == InmateDetail.LegalStatus.DEAD

  private fun InmateDetail.isReleasedFromRemand(): Boolean = isInactiveOut() && sentenceDetail?.releaseDate == null

  private fun InmateDetail.isReleasedFromCustodialSentence(): Boolean =
    isInactiveOut() && sentenceDetail?.releaseDate?.onOrBefore(LocalDate.now()) == true

  private fun InmateDetail.isInactiveOut(): Boolean = activeFlag.not() && inOutStatus == InmateDetail.InOutStatus.OUT

  // TODO flag as event of interest.
  private fun flagEventOfInterest(event: OffenderReleasedEvent) {
    log.info("Unable to determine release reason, flag $event as event of interest")
  }

  private fun List<Allocation>.suspendAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filter { it.status(PrisonerStatus.ACTIVE) }.map { it.autoSuspend(now, "Temporarily released from prison") }
    }.saveAffectedAllocations()

  private fun List<Allocation>.deallocateAndSaveAffectedAllocations(reason: String = "Released from prison") =
    LocalDateTime.now().let { now ->
      this.filterNot { it.status(PrisonerStatus.ENDED) }.map { it.deallocate(now, reason) }
    }.saveAffectedAllocations()

  private fun List<Allocation>.saveAffectedAllocations() =
    repository.saveAllAndFlush(this).toList()
}
