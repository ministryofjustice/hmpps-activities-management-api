package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.SentenceCalcDates
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
  private val prisonApiClient: PrisonApiApplicationClient = mock()
  private val handler = OffenderReleasedEventHandler(prisonApiClient, repository)
  private val prisoner: InmateDetail = mock {
    on { activeFlag } doReturn false
    on { inOutStatus } doReturn InmateDetail.InOutStatus.OUT
  }

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

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(
      previouslyActiveAllocations,
    )

    val successful = handler.handle(offenderTemporaryReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(successful).isTrue

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
      assertThat(it.suspendedBy).isEqualTo("Activities Management Service")
      assertThat(it.suspendedReason).isEqualTo("Temporarily released from prison")
      assertThat(it.suspendedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
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

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(allocations)

    val successful = handler.handle(offenderTemporaryReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(successful).isTrue
    assertThat(allocations[0].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(allocations[1].status(PrisonerStatus.ENDED)).isTrue()
    assertThat(allocations[2].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
  }

  @Test
  fun `un-ended allocations are ended on release from death of prisoner`() {
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

    whenever(prisoner.legalStatus).doReturn(InmateDetail.LegalStatus.DEAD)
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(
      previouslyActiveAllocations,
    )

    val successful = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(successful).isTrue

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo("Dead")
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }

    verify(repository).saveAllAndFlush(any<List<Allocation>>())
  }

  @Test
  fun `un-ended allocations are ended on release from remand`() {
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

    val sentenceCalcDatesNoReleaseDateForRemand: SentenceCalcDates = mock { on { releaseDate } doReturn null }

    prisoner.stub {
      on { sentenceDetail } doReturn sentenceCalcDatesNoReleaseDateForRemand
    }

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(
      previouslyActiveAllocations,
    )

    val successful = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(successful).isTrue

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo("Released")
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    }

    verify(repository).saveAllAndFlush(any<List<Allocation>>())
  }

  @Test
  fun `un-ended allocations are ended on release from custodial`() {
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

    val sentenceCalcDatesReleaseDateTodayForCustodialSentence: SentenceCalcDates =
      mock { on { releaseDate } doReturn LocalDate.now() }

    prisoner.stub {
      on { sentenceDetail } doReturn sentenceCalcDatesReleaseDateTodayForCustodialSentence
    }

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(
      previouslyActiveAllocations,
    )

    val successful = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(successful).isTrue

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo("Released")
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
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

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true)).doReturn(
      Mono.just(
        prisoner,
      ),
    )
    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(allocations)

    val successful = handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456"))

    assertThat(successful).isTrue

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

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(listOf(allocation))

    val successful = handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "12345",
          reason = "UNKNOWN",
          prisonId = pentonvillePrisonCode,
        ),
      ),
    )

    assertThat(successful).isFalse

    assertThat(allocation.status(PrisonerStatus.ACTIVE)).isTrue

    verifyNoInteractions(repository)
  }
}
