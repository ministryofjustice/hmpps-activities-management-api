package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import java.time.LocalDateTime

class OffenderReceivedEventHandlerTest {

  private val repository: AllocationRepository = mock()
  private val handler = OffenderReceivedEventHandler(repository)

  @Test
  fun `only auto-suspended allocations are reactivated on receipt of prisoner`() {
    val now = LocalDateTime.now()

    val autoSuspendedOne =
      allocation().copy(allocationId = 1, prisonerNumber = "123456").autoSuspend(now, "Auto reason")
    val autoSuspendedTwo =
      allocation().copy(allocationId = 2, prisonerNumber = "123456").autoSuspend(now, "Auto Reason")
    val userSuspended =
      allocation().copy(allocationId = 3, prisonerNumber = "123456").userSuspend(now, "User reason", "username")
    val ended = allocation().copy(allocationId = 3, prisonerNumber = "123456").deallocate(now, "Deallocate reason")

    val allocations = listOf(autoSuspendedOne, autoSuspendedTwo, userSuspended, ended)

    assertThat(autoSuspendedOne.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue

    whenever(repository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).thenReturn(
      allocations,
    )

    handler.handle(offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456"))

    assertThat(autoSuspendedOne.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue

    verify(repository).saveAllAndFlush(any<List<Allocation>>())
  }
}
