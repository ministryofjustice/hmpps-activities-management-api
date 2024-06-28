package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceSuspensionDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent

class ActivitiesChangedEventHandlerTest {
  private val rolloutPrison: RolloutPrison = mock {
    on { isActivitiesRolledOut() } doReturn true
  }

  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findByCode(MOORLAND_PRISON_CODE) } doReturn rolloutPrison
  }

  private val allocationRepository: AllocationRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val prisonerAllocationHandler: PrisonerAllocationHandler = mock()
  private val waitingListService: WaitingListService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService = mock()

  private val handler = ActivitiesChangedEventHandler(
    rolloutPrisonRepository,
    allocationRepository,
    prisonerSearchApiClient,
    prisonerAllocationHandler,
    TransactionHandler(),
    waitingListService,
    outboundEventsService,
    attendanceSuspensionDomainService,
  )

  @BeforeEach
  fun beforeEach() {
    whenever(
      allocationRepository.existAtPrisonForPrisoner(
        any(),
        any(),
        eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()),
      ),
    ) doReturn true
  }

  @Test
  fun `event is ignored for an inactive prison`() {
    rolloutPrison.stub { on { isActivitiesRolledOut() } doReturn false }

    assertThat(handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, MOORLAND_PRISON_CODE)).isSuccess()).isTrue

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
  }

  @Test
  fun `event is ignored when no matching prison`() {
    rolloutPrisonRepository.stub { on { findByCode(MOORLAND_PRISON_CODE) } doReturn null }

    assertThat(handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, MOORLAND_PRISON_CODE)).isSuccess()).isTrue

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verifyNoInteractions(allocationRepository)
  }

  @Test
  fun `active, pending and suspended allocations starting on or before today are auto-suspended on suspend action`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE),
      allocation().copy(allocationId = 2, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE),
      allocation().copy(allocationId = 3, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.SUSPENDED),
      allocation().copy(allocationId = 4, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.PENDING, startDate = TimeSource.today()),
      allocation().copy(allocationId = 5, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.PENDING, startDate = TimeSource.tomorrow()),
    )

    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        MOORLAND_PRISON_CODE,
        "123456",
        PrisonerStatus.ACTIVE,
        PrisonerStatus.PENDING,
        PrisonerStatus.SUSPENDED,
      ),
    ) doReturn allocations

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, MOORLAND_PRISON_CODE))

    outcome.isSuccess() isBool true

    allocations.subList(0, 4).forEach {
      it.prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED
      it.suspendedBy isEqualTo "Activities Management Service"
      it.suspendedReason isEqualTo "Temporarily released or transferred"
      it.suspendedTime isCloseTo TimeSource.now()
    }

    allocations.last().prisonerStatus isEqualTo PrisonerStatus.PENDING
  }

  @Test
  fun `future attendances are suspended on suspend action`() {
    val allocation = allocation().copy(allocationId = 1, prisonerNumber = "123456")
    listOf(allocation).also {
      whenever(
        allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
          MOORLAND_PRISON_CODE,
          "123456",
          PrisonerStatus.ACTIVE,
          PrisonerStatus.PENDING,
          PrisonerStatus.SUSPENDED,
        ),
      ) doReturn it
    }

    handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, MOORLAND_PRISON_CODE))

    verify(attendanceSuspensionDomainService).autoSuspendFutureAttendancesForAllocation(any(), eq(allocation))
  }

  @Test
  fun `allocations are not deallocated on 'END' when prisoner not found`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber(any())) doReturn null

    assertThatThrownBy {
      handler.handle(activitiesChangedEvent("123456", Action.END, MOORLAND_PRISON_CODE))
    }.isInstanceOf(NullPointerException::class.java)
      .hasMessage("Prisoner search lookup failed for prisoner 123456")

    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `released (inactive out) prisoner is deallocated on 'END' with reason 'RELEASED'`() {
    mock<Prisoner> {
      on { status } doReturn "INACTIVE OUT"
    }.also { permanentlyReleasedPrisoner ->
      whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn permanentlyReleasedPrisoner
    }

    handler.handle(activitiesChangedEvent("123456", Action.END, MOORLAND_PRISON_CODE))

    verify(prisonerAllocationHandler).deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `released (at other prison) prisoner is deallocated on 'END' with reason 'TEMPORARILY RELEASED'`() {
    mock<Prisoner> {
      on { status } doReturn "ACTIVE IN"
      on { prisonId } doReturn PENTONVILLE_PRISON_CODE
    }.also { permanentlyReleasedPrisoner ->
      whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn permanentlyReleasedPrisoner
    }

    handler.handle(activitiesChangedEvent("123456", Action.END, MOORLAND_PRISON_CODE))

    verify(prisonerAllocationHandler).deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.TEMPORARILY_RELEASED)
  }

  @Test
  fun `released (but still at same prison) prisoner is deallocated on 'END' with reason 'OTHER`() {
    mock<Prisoner> {
      on { status } doReturn "ACTIVE IN"
      on { prisonId } doReturn MOORLAND_PRISON_CODE
    }.also { permanentlyReleasedPrisoner ->
      whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn permanentlyReleasedPrisoner
    }

    handler.handle(activitiesChangedEvent("123456", Action.END, MOORLAND_PRISON_CODE))

    verify(prisonerAllocationHandler).deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.OTHER)
  }

  @Test
  fun `no interactions when released prisoner has no allocations of interest`() {
    whenever(
      allocationRepository.existAtPrisonForPrisoner(
        any(),
        any(),
        eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()),
      ),
    ) doReturn false

    handler.handle(activitiesChangedEvent("123456", Action.END, MOORLAND_PRISON_CODE)).also { it.isSuccess() isBool true }

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(prisonerAllocationHandler)
  }
}
