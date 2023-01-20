package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations

class AllocationServiceTest {
  private val repository: AllocationRepository = mock()
  private val service: AllocationsService = AllocationsService(repository)

  @Test
  fun `find allocations for collection of prisoners`() {
    val allocations = activityEntity().schedules().flatMap { it.allocations }.also { assertThat(it).isNotEmpty }
    val prisonNumbers = allocations.map { it.prisonerNumber }

    whenever(repository.findByPrisonCodeAndPrisonerNumbers("MDI", prisonNumbers)).thenReturn(allocations)

    assertThat(
      service.findByPrisonCodeAndPrisonerNumbers(
        "MDI",
        prisonNumbers.toSet()
      )
    ).isEqualTo(allocations.toModelPrisonerAllocations())
  }
}
