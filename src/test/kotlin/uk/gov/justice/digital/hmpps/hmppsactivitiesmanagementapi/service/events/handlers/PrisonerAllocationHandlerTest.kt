package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonerAllocationHandlerTest {
  private val allocationRepository: AllocationRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val handler = PrisonerAllocationHandler(
    allocationRepository,
    attendanceRepository,
    TransactionHandler(),
    outboundEventsService,
  )

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

    val pendingAllocation =
      allocation(startDate = TimeSource.tomorrow()).also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }

    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        MOORLAND_PRISON_CODE,
        "123456",
        *PrisonerStatus.allExcuding(
          PrisonerStatus.ENDED,
        ),
      ),
    ) doReturn previouslyActiveAllocations.plus(pendingAllocation)

    handler.deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.TEMPORARILY_RELEASED)

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.TEMPORARILY_RELEASED)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }
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

    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        MOORLAND_PRISON_CODE,
        "123456",
        *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
      ),
    ) doReturn previouslyActiveAllocations

    handler.deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `only un-ended allocations are ended on release of prisoner`() {
    val previouslySuspendedAllocation = allocation().copy(allocationId = 2, prisonerNumber = "123456")
      .also { it.autoSuspend(LocalDateTime.now(), "reason") }
    val previouslyActiveAllocation = allocation().copy(allocationId = 3, prisonerNumber = "123456")

    val allocations = listOf(previouslySuspendedAllocation, previouslyActiveAllocation)

    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        MOORLAND_PRISON_CODE,
        "123456",
        *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
      ),
    ) doReturn allocations

    handler.deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)

    assertThat(previouslySuspendedAllocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(previouslyActiveAllocation.status(PrisonerStatus.ENDED)).isTrue
  }

  @Test
  fun `only future attendances are removed on release`() {
    val allocation = allocation().copy(allocationId = 1, prisonerNumber = "123456")
    listOf(allocation).also {
      whenever(
        allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
          MOORLAND_PRISON_CODE,
          "123456",
          *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
        ),
      ) doReturn it
    }

    val pastScheduledInstance = mock<ScheduledInstance> { on { isFuture(any()) } doReturn false }
    val pastAttendance = attendanceFor(pastScheduledInstance)

    val futureScheduledInstance = mock<ScheduledInstance> { on { isFuture(any()) } doReturn true }
    val futureAttendance = attendanceFor(futureScheduledInstance)

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForAllocation(
        sessionDate = LocalDate.now(),
        activityScheduleId = allocation.activitySchedule.activityScheduleId,
        prisonerNumber = "123456",
      ),
    ) doReturn listOf(pastAttendance, futureAttendance)

    handler.deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)

    verify(pastScheduledInstance, never()).remove(pastAttendance)
    verify(futureScheduledInstance).remove(futureAttendance)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_DELETED, futureAttendance.attendanceId)
  }

  private fun attendanceFor(instance: ScheduledInstance): Attendance = mock {
    on { scheduledInstance } doReturn instance
    on { editable() } doReturn true
  }
}
