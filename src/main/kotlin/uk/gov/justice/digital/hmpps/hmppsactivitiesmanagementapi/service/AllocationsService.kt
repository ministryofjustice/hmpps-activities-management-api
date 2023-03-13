package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations

@Service
class AllocationsService(private val allocationRepository: AllocationRepository) {
  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonNumbers: Set<String>, activeOnly: Boolean = true) =
    allocationRepository
      .findByPrisonCodeAndPrisonerNumbers(prisonCode, prisonNumbers.toList())
      .filter { !activeOnly || it.status(PrisonerStatus.ACTIVE) }
      .toModelPrisonerAllocations()

  fun getAllocationById(id: Long) = allocationRepository.findOrThrowNotFound(id).toModel()
}
