package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.LocalDate

@Service
class AllocationsService(private val allocationRepository: AllocationRepository) {
  fun findByPrisonCodeAndPrisonerNumbers(prisonCode: String, prisonNumbers: Set<String>, activeOnly: Boolean = true) =
    LocalDate.now().let { today ->
      allocationRepository
        .findByPrisonCodeAndPrisonerNumbers(prisonCode, prisonNumbers.toList())
        .filter { !activeOnly || it.isActive(today) }
        .toModelPrisonerAllocations()
    }

  fun getAllocationById(id: Long) = allocationRepository.findOrThrowNotFound(id).toModel()
}
