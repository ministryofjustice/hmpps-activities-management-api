package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import java.time.LocalDateTime

@Service
class OffenderEventsOfInterestService(private val repository: AllocationRepository) {

  fun temporarilyReleaseOffender(prisonCode: String, prisonerNumber: String) {
    repository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).suspendAndSaveAffectedAllocations()
  }

  fun permanentlyReleaseOffender(prisonCode: String, prisonerNumber: String) {
    repository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).deallocateAndSaveAffectedAllocations()
  }

  private fun List<Allocation>.suspendAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filter { it.status(PrisonerStatus.ACTIVE) }.map { it.suspend(now, "Temporarily released from prison") }
    }.saveAffectedAllocations()

  private fun List<Allocation>.deallocateAndSaveAffectedAllocations() =
    LocalDateTime.now().let { now ->
      this.filterNot { it.status(PrisonerStatus.ENDED) }.map { it.deallocate(now, "Released from prison") }
    }.saveAffectedAllocations()

  private fun List<Allocation>.saveAffectedAllocations() =
    repository.saveAllAndFlush(this).toList()
}
