package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderReceivedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonApiClient: PrisonApiApplicationClient = mock()
  private val activeMoorlandPrisoner: InmateDetail = mock {
    on { status } doReturn "ACTIVE IN"
    on { agencyId } doReturn moorlandPrisonCode
  }

  private val handler = OffenderReceivedEventHandler(rolloutPrisonRepository, allocationRepository, prisonApiClient)

  @BeforeEach
  fun beforeTests() {
    reset(rolloutPrisonRepository, allocationRepository)
    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = true,
          activitiesRolloutDate = LocalDate.now().plusDays(-1),
        )
    }
  }

  @Test
  fun `inbound received event is not handled for an inactive prison`() {
    val inboundEvent = offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456")
    reset(rolloutPrisonRepository)
    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    val outcome = handler.handle(inboundEvent)

    assertThat(outcome.isSuccess()).isTrue
    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(allocationRepository)
  }

  @Test
  fun `auto-suspended allocations are reactivated on receipt of prisoner`() {
    val now = LocalDateTime.now()

    val autoSuspendedOne =
      allocation().copy(allocationId = 1, prisonerNumber = "123456").autoSuspend(now, "Auto reason")
    val autoSuspendedTwo =
      allocation().copy(allocationId = 2, prisonerNumber = "123456").autoSuspend(now, "Auto Reason")
    val userSuspended =
      allocation().copy(allocationId = 3, prisonerNumber = "123456").userSuspend(now, "User reason", "username")
    val ended = allocation().copy(allocationId = 3, prisonerNumber = "123456").deallocate(now, DeallocationReason.ENDED)

    val allocations = listOf(autoSuspendedOne, autoSuspendedTwo, userSuspended, ended)

    assertThat(autoSuspendedOne.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue

    prisonApiClient.stub {
      on { getPrisonerDetails("123456") } doReturn Mono.just(activeMoorlandPrisoner)
    }

    allocationRepository.stub {
      on { findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456") } doReturn allocations
    }

    val outcome = handler.handle(offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456"))

    assertThat(outcome.isSuccess()).isTrue
    assertThat(autoSuspendedOne.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue

    verify(allocationRepository).saveAllAndFlush(any<List<Allocation>>())
  }
}
