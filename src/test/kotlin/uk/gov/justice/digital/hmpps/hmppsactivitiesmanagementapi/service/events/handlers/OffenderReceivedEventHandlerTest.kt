package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceSuspensionDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderReceivedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val handler =
    OffenderReceivedEventHandler(rolloutPrisonRepository, allocationRepository, prisonerSearchApiClient, attendanceSuspensionDomainService, TransactionHandler(), outboundEventsService)

  @BeforeEach
  fun beforeTests() {
    reset(rolloutPrisonRepository, allocationRepository)
    rolloutPrisonRepository.stub {
      on { findByCode(MOORLAND_PRISON_CODE) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = true,
          activitiesRolloutDate = LocalDate.now().plusDays(-1),
        )
    }
  }

  @Test
  fun `inbound received event is not handled for an inactive prison`() {
    val inboundEvent = offenderReceivedFromTemporaryAbsence(MOORLAND_PRISON_CODE, "123456")
    reset(rolloutPrisonRepository)
    rolloutPrisonRepository.stub {
      on { findByCode(MOORLAND_PRISON_CODE) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    val outcome = handler.handle(inboundEvent)

    assertThat(outcome.isSuccess()).isTrue
    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
  }

  @Test
  fun `auto-suspended allocations are reactivated on receipt of prisoner`() {
    val now = LocalDateTime.now()

    val autoSuspendedOne =
      allocation().copy(allocationId = 1, prisonerNumber = "123456").autoSuspend(now, "Auto reason")
    val autoSuspendedTwo =
      allocation().copy(allocationId = 2, prisonerNumber = "123456").apply {
        addPlannedSuspension(
          PlannedSuspension(
            allocation = this,
            plannedStartDate = this.startDate,
            plannedReason = "Planned reason",
            plannedBy = "Test",
          ),
        )
      }.autoSuspend(now, "Auto Reason")
    val userSuspended =
      allocation().copy(allocationId = 3, prisonerNumber = "123456").apply {
        addPlannedSuspension(
          PlannedSuspension(
            allocation = this,
            plannedStartDate = this.startDate,
            plannedReason = "Planned reason",
            plannedBy = "Test",
          ),
        )
      }.activatePlannedSuspension()
    val ended = allocation().copy(allocationId = 3, prisonerNumber = "123456").deallocateNowWithReason(DeallocationReason.ENDED)

    val allocations = listOf(autoSuspendedOne, autoSuspendedTwo, userSuspended, ended)

    assertThat(autoSuspendedOne.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn activeInMoorlandPrisoner.copy(prisonerNumber = "123456")
    }

    allocationRepository.stub {
      on { findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, "123456") } doReturn allocations
    }

    val outcome = handler.handle(offenderReceivedFromTemporaryAbsence(MOORLAND_PRISON_CODE, "123456"))

    assertThat(outcome.isSuccess()).isTrue
    assertThat(autoSuspendedOne.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue
  }

  @Test
  fun `future attendances are unsuspended on receipt of prisoner`() {
    val allocation = allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.AUTO_SUSPENDED)
    listOf(allocation).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, "123456")) doReturn it
    }

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn activeInMoorlandPrisoner.copy(prisonerNumber = "123456")
    }

    handler.handle(offenderReceivedFromTemporaryAbsence(MOORLAND_PRISON_CODE, "123456"))

    verify(attendanceSuspensionDomainService).resetFutureSuspendedAttendancesForAllocation(any(), eq(allocation))
  }
}
