package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ActivitiesChangedEventHandlerTest {
  private val rolloutPrison: RolloutPrison = mock {
    on { isActivitiesRolledOut() } doReturn true
  }

  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findByCode(moorlandPrisonCode) } doReturn rolloutPrison
  }

  private val allocationRepository: AllocationRepository = mock()

  private val attendanceRepository: AttendanceRepository = mock()

  private val attendanceReasonRepository: AttendanceReasonRepository = mock()

  private val handler = ActivitiesChangedEventHandler(
    rolloutPrisonRepository,
    allocationRepository,
    attendanceRepository,
    attendanceReasonRepository,
  )

  @Test
  fun `event is ignored for an inactive prison`() {
    rolloutPrison.stub { on { isActivitiesRolledOut() } doReturn false }

    assertThat(handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode)).isSuccess()).isTrue

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(allocationRepository)
  }

  @Test
  fun `event is ignored when no matching prison`() {
    rolloutPrisonRepository.stub { on { findByCode(moorlandPrisonCode) } doReturn null }

    assertThat(handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode)).isSuccess()).isTrue

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(allocationRepository)
  }

  @Test
  fun `allocations are auto-suspended on suspend action`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.suspendedBy).isNull()
      assertThat(it.suspendedReason).isNull()
      assertThat(it.suspendedTime).isNull()
    }

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456"))
      .doReturn(allocations)

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode))

    assertThat(outcome.isSuccess()).isTrue

    allocations.forEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
      assertThat(it.suspendedBy).isEqualTo("Activities Management Service")
      assertThat(it.suspendedReason).isEqualTo("Temporary absence")
      assertThat(it.suspendedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `only future attendances are suspended on suspend action`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456")).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")) doReturn it
    }

    val suspendedAttendanceReason = mock<AttendanceReason>().also {
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)) doReturn it
    }

    val historicAttendance = mock<Attendance>().also {
      val scheduledInstance: ScheduledInstance = mock {
        on { sessionDate } doReturn TimeSource.today()
        on { startTime } doReturn LocalTime.now().minusMinutes(1)
      }
      whenever(it.scheduledInstance) doReturn scheduledInstance
      whenever(it.editable()) doReturn true
    }

    val todaysFutureAttendance = mock<Attendance>().also {
      val scheduledInstance: ScheduledInstance = mock {
        on { startTime } doReturn LocalTime.now().plusMinutes(1)
        on { sessionDate } doReturn TimeSource.today()
      }
      whenever(it.scheduledInstance) doReturn scheduledInstance
      whenever(it.editable()) doReturn true
    }

    val tomorrowsFutureAttendance = mock<Attendance>().also {
      val scheduledInstance: ScheduledInstance = mock {
        on { startTime } doReturn LocalTime.now().plusMinutes(1)
        on { sessionDate } doReturn TimeSource.tomorrow()
      }
      whenever(it.scheduledInstance) doReturn scheduledInstance
      whenever(it.editable()) doReturn true
    }

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        moorlandPrisonCode,
        LocalDate.now(),
        AttendanceStatus.WAITING,
        "123456",
      ),
    ) doReturn listOf(historicAttendance, todaysFutureAttendance, tomorrowsFutureAttendance)

    handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode))

    verify(historicAttendance, never()).completeWithoutPayment(suspendedAttendanceReason)
    verify(todaysFutureAttendance).completeWithoutPayment(suspendedAttendanceReason)
    verify(tomorrowsFutureAttendance).completeWithoutPayment(suspendedAttendanceReason)
  }

  @Test
  fun `only active allocations are auto-suspended on suspend action`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456")
        .also { it.deallocateNowWithReason(DeallocationReason.ENDED) },
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    )

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")).doReturn(allocations)

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode))

    assertThat(outcome.isSuccess()).isTrue
    assertThat(allocations[0].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    assertThat(allocations[1].status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocations[2].status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
  }

  @Test
  fun `allocations are ended on end action`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456"),
      allocation().copy(allocationId = 2, prisonerNumber = "123456"),
      allocation().copy(allocationId = 3, prisonerNumber = "123456"),
    ).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
      assertThat(it.deallocatedBy).isNull()
      assertThat(it.deallocatedReason).isNull()
      assertThat(it.deallocatedTime).isNull()
    }

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456"))
      .doReturn(allocations)

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode))

    assertThat(outcome.isSuccess()).isTrue

    allocations.forEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.TEMPORARY_ABSENCE)
      assertThat(it.deallocatedTime)
        .isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `future attendances are not removed on suspend`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456")).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")) doReturn it
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

    handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode))

    verify(todaysHistoricScheduledInstance, never()).remove(todaysHistoricAttendance)
    verify(todaysFuturescheduledInstance, never()).remove(todaysFutureAttendance)
    verify(tomorrowsScheduledInstance, never()).remove(tomorrowsFutureAttendance)
  }

  @Test
  fun `only future attendances are removed on end`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456")).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")) doReturn it
    }

    val todaysHistoricScheduledInstance = scheduledInstanceOn(TimeSource.today(), LocalTime.now().minusMinutes(1))
    val todaysHistoricAttendance = attendanceFor(todaysHistoricScheduledInstance)

    val todaysFutureScheduledInstance = scheduledInstanceOn(TimeSource.today(), LocalTime.now().plusMinutes(1))
    val todaysFutureAttendance = attendanceFor(todaysFutureScheduledInstance)

    val tomorrowsScheduledInstance = scheduledInstanceOn(TimeSource.tomorrow(), LocalTime.now().plusMinutes(1))
    val tomorrowsFutureAttendance = attendanceFor(tomorrowsScheduledInstance)

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = moorlandPrisonCode,
        sessionDate = LocalDate.now(),
        prisonerNumber = "123456",
      ),
    ) doReturn listOf(todaysHistoricAttendance, todaysFutureAttendance, tomorrowsFutureAttendance)

    handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode))

    verify(todaysHistoricScheduledInstance, never()).remove(todaysHistoricAttendance)
    verify(todaysFutureScheduledInstance).remove(todaysFutureAttendance)
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
