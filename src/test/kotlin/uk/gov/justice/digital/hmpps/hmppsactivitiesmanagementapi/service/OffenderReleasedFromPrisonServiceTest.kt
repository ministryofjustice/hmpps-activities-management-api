package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import java.time.LocalDateTime

class OffenderReleasedFromPrisonServiceTest {

  private val repository: AllocationRepository = mock()
  private val service = OffenderReleasedFromPrisonService(repository)

  @Test
  fun `active allocations are auto-suspended on temporary release of prisoner`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach { assertThat(it.status(PrisonerStatus.ACTIVE)) }

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(allocations)

    service.temporarilyReleaseOffender(moorlandPrisonCode, "123456")

    allocations.forEach { assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue }

    verify(repository).saveAllAndFlush(any<List<Allocation>>())
  }

  @Test
  fun `only active allocations are auto-suspended on temporary release of prisoner`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456").also { it.deallocate(LocalDateTime.now()) },
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    )

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(allocations)

    service.temporarilyReleaseOffender(moorlandPrisonCode, "123456")

    assertThat(allocations[0].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue()
    assertThat(allocations[1].status(PrisonerStatus.ENDED)).isTrue()
    assertThat(allocations[2].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue()
  }
}
