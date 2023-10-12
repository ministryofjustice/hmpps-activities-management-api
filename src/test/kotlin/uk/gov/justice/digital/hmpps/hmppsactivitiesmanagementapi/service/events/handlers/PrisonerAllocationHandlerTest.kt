package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class PrisonerAllocationHandlerTest {
  private val allocationRepository: AllocationRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val waitingListService: WaitingListService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val handler = PrisonerAllocationHandler(
    allocationRepository,
    attendanceRepository,
    waitingListService,
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
        moorlandPrisonCode,
        "123456",
        *PrisonerStatus.allExcuding(
          PrisonerStatus.ENDED,
        ),
      ),
    ) doReturn previouslyActiveAllocations.plus(pendingAllocation)

    handler.deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }

    verify(waitingListService).declinePendingOrApprovedApplications(
      moorlandPrisonCode,
      "123456",
      "Released",
      "Activities Management Service",
    )
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
        moorlandPrisonCode,
        "123456",
        *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
      ),
    ) doReturn previouslyActiveAllocations

    handler.deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)

    previouslyActiveAllocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }

    verify(waitingListService).declinePendingOrApprovedApplications(
      moorlandPrisonCode,
      "123456",
      "Released",
      "Activities Management Service",
    )
  }

  @Test
  fun `only un-ended allocations are ended on release of prisoner`() {
    val previouslySuspendedAllocation = allocation().copy(allocationId = 2, prisonerNumber = "123456")
      .also { it.autoSuspend(LocalDateTime.now(), "reason") }
    val previouslyActiveAllocation = allocation().copy(allocationId = 3, prisonerNumber = "123456")

    val allocations = listOf(previouslySuspendedAllocation, previouslyActiveAllocation)

    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        moorlandPrisonCode,
        "123456",
        *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
      ),
    ) doReturn allocations

    handler.deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)

    assertThat(previouslySuspendedAllocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(previouslyActiveAllocation.status(PrisonerStatus.ENDED)).isTrue

    verify(waitingListService).declinePendingOrApprovedApplications(
      moorlandPrisonCode,
      "123456",
      "Released",
      "Activities Management Service",
    )
  }

  @Test
  fun `only future attendances are removed on release`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456")).also {
      whenever(
        allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
          moorlandPrisonCode,
          "123456",
          *PrisonerStatus.allExcuding(PrisonerStatus.ENDED),
        ),
      ) doReturn it
    }

    val todaysHistoricScheduledInstance = scheduledInstanceOn(TimeSource.today(), LocalTime.now().minusMinutes(1))
    val todaysHistoricAttendance = attendanceFor(todaysHistoricScheduledInstance)

    val todaysFuturescheduledInstance = scheduledInstanceOn(TimeSource.today(), LocalTime.now().plusMinutes(1))
    val todaysFutureAttendance = attendanceFor(todaysFuturescheduledInstance)

    val tomorrowsScheduledInstance = scheduledInstanceOn(TimeSource.tomorrow(), LocalTime.now().plusMinutes(1))
    val tomorrowsFutureAttendance = attendanceFor(tomorrowsScheduledInstance)

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = moorlandPrisonCode,
        sessionDate = LocalDate.now(),
        prisonerNumber = "123456",
      ),
    ) doReturn listOf(todaysHistoricAttendance, todaysFutureAttendance, tomorrowsFutureAttendance)

    handler.deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)

    verify(todaysHistoricScheduledInstance, never()).remove(todaysHistoricAttendance)
    verify(todaysFuturescheduledInstance).remove(todaysFutureAttendance)
    verify(tomorrowsScheduledInstance).remove(tomorrowsFutureAttendance)
  }

  private fun scheduledInstanceOn(date: LocalDate, time: LocalTime): ScheduledInstance = mock {
    on { sessionDate } doReturn date
    on { startTime } doReturn time
  }

  private fun attendanceFor(instance: ScheduledInstance): Attendance = mock {
    on { scheduledInstance } doReturn instance
    on { editable() } doReturn true
  }
}
