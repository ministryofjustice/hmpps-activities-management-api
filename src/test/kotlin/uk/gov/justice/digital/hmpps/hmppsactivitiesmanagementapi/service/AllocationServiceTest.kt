package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.util.Optional

class AllocationServiceTest {
  private val repository: AllocationRepository = mock()
  private val service: AllocationsService = AllocationsService(repository)

  @Test
  fun `find allocations for collection of prisoners`() {
    val allocations = activityEntity().schedules().flatMap { it.allocations() }.also { assertThat(it).isNotEmpty }
    val prisonNumbers = allocations.map { it.prisonerNumber }

    whenever(repository.findByPrisonCodeAndPrisonerNumbers("MDI", prisonNumbers)).thenReturn(allocations)

    assertThat(
      service.findByPrisonCodeAndPrisonerNumbers(
        "MDI",
        prisonNumbers.toSet()
      )
    ).isEqualTo(allocations.toModelPrisonerAllocations())
  }

  @Test
  fun `transformed allocation returned when find by id`() {
    val expected = allocation()

    whenever(repository.findById(expected.allocationId)).thenReturn(Optional.of(expected))

    assertThat(service.getAllocationById(expected.allocationId)).isEqualTo(expected.toModel())
  }

  @Test
  fun `find by id throws entity not found for unknown allocation`() {
    whenever(repository.findById(any())).thenReturn(Optional.empty())

    assertThatThrownBy {
      service.getAllocationById(1)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Allocation 1 not found")
  }
}
