package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

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

  fun process(event: InboundEvent, prisonCode: String, prisonerNumber: String) {
    when (event) {
      InboundEvent.OFFENDER_RECEIVED -> reactivateSuspendedOffenderAllocations(prisonCode, prisonerNumber)
      InboundEvent.OFFENDER_RELEASED -> deallocateOffenderAllocations(prisonCode, prisonerNumber)
      InboundEvent.OFFENDER_TEMPORARILY_RELEASED -> suspendOffenderAllocations(prisonCode, prisonerNumber)
    }
  }

  private fun suspendOffenderAllocations(prisonCode: String, prisonerNumber: String) {
    repository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).suspendAndSaveAffectedAllocations()
      .also { log.info("Suspended ${it.size} allocations for prisoner $prisonerNumber at prison $prisonCode.") }
  }

  private fun deallocateOffenderAllocations(prisonCode: String, prisonerNumber: String) {
    repository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).deallocateAndSaveAffectedAllocations()
      .also { log.info("Deallocated prisoner $prisonerNumber at prison $prisonCode from ${it.size} allocations.") }
  }

  private fun reactivateSuspendedOffenderAllocations(prisonCode: String, prisonerNumber: String) {
    repository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).reactivateAndSaveAffectedAllocations()
      .also { log.info("Reactivated ${it.size} allocations for prisoner $prisonerNumber at prison $prisonCode.") }
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

enum class InboundEvent {
  OFFENDER_RECEIVED,
  OFFENDER_RELEASED,
  OFFENDER_TEMPORARILY_RELEASED,
}
