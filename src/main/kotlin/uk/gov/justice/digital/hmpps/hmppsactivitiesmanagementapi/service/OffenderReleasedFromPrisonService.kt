package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import java.time.LocalDateTime

@Service
class OffenderReleasedFromPrisonService(private val allocationRepository: AllocationRepository) {

  fun temporarilyReleaseOffender(prisonCode: String, prisonerNumber: String) {
    allocationRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber)
      .temporarilyReleaseActiveOffendersAllocations()
      .also { allocationRepository.saveAllAndFlush(it) }
  }

  private fun List<Allocation>.temporarilyReleaseActiveOffendersAllocations() =
    LocalDateTime.now().let { now ->
      this.filter { it.status(PrisonerStatus.ACTIVE) }.map { it.suspend(now, "Temporarily released from prison") }
    }

  // TODO ability to permanently release offender to be added in a separate ticket
}
