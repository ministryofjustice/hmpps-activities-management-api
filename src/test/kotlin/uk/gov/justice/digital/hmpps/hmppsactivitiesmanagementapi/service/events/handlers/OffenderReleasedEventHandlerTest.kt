package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderTemporaryReleasedEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OffenderReleasedEventHandlerTest {

  private val repository: AllocationRepository = mock()
  private val handler = OffenderReleasedEventHandler(repository)

  @Test
  fun `active allocations are auto-suspended on temporary release of prisoner`() {
    val previouslyActiveAllocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.suspendedBy).isNull()
      assertThat(it.suspendedReason).isNull()
      assertThat(it.suspendedTime).isNull()
    }

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(
      previouslyActiveAllocations,
    )

    handler.handle(offenderTemporaryReleasedEvent(moorlandPrisonCode, "123456"))

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
      assertThat(it.suspendedBy).isEqualTo("SYSTEM")
      assertThat(it.suspendedReason).isEqualTo("Temporarily released from prison")
      assertThat(it.suspendedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }

    verify(repository).saveAllAndFlush(any<List<Allocation>>())
  }

  @Test
  fun `only active allocations are auto-suspended on temporary release of prisoner`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456")
        .also { it.deallocate(LocalDateTime.now(), "reason") },
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    )

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(allocations)

    handler.handle(offenderTemporaryReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(allocations[0].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(allocations[1].status(PrisonerStatus.ENDED)).isTrue()
    assertThat(allocations[2].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
  }

  @Test
  fun `un-ended allocations are ended on release of prisoner`() {
    val previouslyActiveAllocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.deallocatedBy).isNull()
      assertThat(it.deallocatedReason).isNull()
      assertThat(it.deallocatedTime).isNull()
    }

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(
      previouslyActiveAllocations,
    )

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("SYSTEM")
      assertThat(it.deallocatedReason).isEqualTo("Released from prison")
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }

    verify(repository).saveAllAndFlush(any<List<Allocation>>())
  }

  @Test
  fun `only un-ended allocations are ended on release of prisoner`() {
    val yesterday = LocalDate.now().atStartOfDay()

    val previouslyEndedAllocation = allocation().copy(allocationId = 1, prisonerNumber = "123456")
      .also { it.deallocate(yesterday, "reason") }
    val previouslySuspendedAllocation = allocation().copy(allocationId = 2, prisonerNumber = "123456")
      .also { it.autoSuspend(LocalDateTime.now(), "reason") }
    val previouslyActiveAllocation = allocation().copy(allocationId = 3, prisonerNumber = "123456")

    val allocations = listOf(previouslyEndedAllocation, previouslySuspendedAllocation, previouslyActiveAllocation)

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(allocations)

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    with(previouslyEndedAllocation) {
      assertThat(status(PrisonerStatus.ENDED)).isTrue
      assertThat(deallocatedTime).isEqualTo(yesterday)
    }

    assertThat(previouslySuspendedAllocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(previouslyActiveAllocation.status(PrisonerStatus.ENDED)).isTrue
  }

  @Test
  fun `allocation is unmodified for unknown release event`() {
    val allocation = allocation().copy(allocationId = 1, prisonerNumber = "123456")
      .also { assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue() }

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(listOf(allocation))

    handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "12345",
          reason = "UNKNOWN",
          prisonId = pentonvillePrisonCode,
        ),
      ),
    )

    assertThat(allocation.status(PrisonerStatus.ACTIVE)).isTrue

    verifyNoInteractions(repository)
  }
}
