package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDateTime

class PrisonerReceiveHandlerTest {
  private val allocationRepository: AllocationRepository = Mockito.mock()
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService = Mockito.mock()
  private val outboundEventsService: OutboundEventsService = Mockito.mock()

  private val handler = PrisonerReceivedHandler(
    allocationRepository,
    attendanceSuspensionDomainService,
    TransactionHandler(),
    outboundEventsService,
  )

  @BeforeEach
  fun beforeTests() {
    reset(allocationRepository)
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
            plannedBy = "Test",
          ),
        )
      }.activatePlannedSuspension()
    val ended = allocation().copy(allocationId = 3, prisonerNumber = "123456").deallocateNowWithReason(
      DeallocationReason.ENDED,
    )

    val allocations = listOf(autoSuspendedOne, autoSuspendedTwo, userSuspended, ended)

    assertThat(autoSuspendedOne.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue

    allocationRepository.stub {
      on { findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, "123456") } doReturn allocations
    }

    handler.receivePrisoner(MOORLAND_PRISON_CODE, "123456")

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

    handler.receivePrisoner(MOORLAND_PRISON_CODE, "123456")

    verify(attendanceSuspensionDomainService).resetAutoSuspendedFutureAttendancesForAllocation(any(), eq(allocation))
  }
}
