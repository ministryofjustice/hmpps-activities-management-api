package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.LocalTime

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
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val prisonerAllocationHandler: PrisonerAllocationHandler = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val handler = ActivitiesChangedEventHandler(
    rolloutPrisonRepository,
    allocationRepository,
    attendanceRepository,
    attendanceReasonRepository,
    prisonerSearchApiClient,
    prisonerAllocationHandler,
    TransactionHandler(),
    outboundEventsService,
  )

  @BeforeEach
  fun beforeEach() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn true
  }

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
  fun `active allocations and pending allocations starting on or before today are auto-suspended on suspend action`() {
    val allocations = listOf(
      allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE),
      allocation().copy(allocationId = 2, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.ACTIVE),
      allocation().copy(
        allocationId = 3,
        prisonerNumber = "123456",
        prisonerStatus = PrisonerStatus.PENDING,
        startDate = TimeSource.today(),
      ),
      allocation().copy(
        allocationId = 4,
        prisonerNumber = "123456",
        prisonerStatus = PrisonerStatus.PENDING,
        startDate = TimeSource.tomorrow(),
      ),
    )

    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        moorlandPrisonCode,
        "123456",
        PrisonerStatus.ACTIVE,
        PrisonerStatus.PENDING,
      ),
    ) doReturn allocations

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode))

    outcome.isSuccess() isBool true

    allocations.subList(0, 2).forEach {
      it.prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED
      it.suspendedBy isEqualTo "Activities Management Service"
      it.suspendedReason isEqualTo "Temporary absence"
      it.suspendedTime isCloseTo TimeSource.now()
    }

    allocations.last().prisonerStatus isEqualTo PrisonerStatus.PENDING
  }

  @Test
  fun `only future attendances are suspended on suspend action`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456")).also {
      whenever(
        allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
          moorlandPrisonCode,
          "123456",
          PrisonerStatus.ACTIVE,
          PrisonerStatus.PENDING,
        ),
      ) doReturn it
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
    verify(outboundEventsService, times(2)).send(eq(OutboundEvent.PRISONER_ATTENDANCE_AMENDED), any())
  }

  @Test
  fun `allocations are not auto-suspended if a runtime error occurs`() {
    whenever(
      allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
        moorlandPrisonCode,
        "123456",
        PrisonerStatus.ACTIVE,
        PrisonerStatus.PENDING,
      ),
    ) doThrow RuntimeException("Error")

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.SUSPEND, moorlandPrisonCode))

    outcome.isSuccess() isBool false
    outcome.message isEqualTo "An error occurred whilst trying to suspend prisoner 123456"
  }

  @Test
  fun `temporarily released prisoner is deallocated on 'END'`() {
    mock<Prisoner> {
      on { status } doReturn "ACTIVE OUT"
      on { prisonId } doReturn moorlandPrisonCode
    }.also { prisoner ->
      whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn prisoner
    }

    handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode)).also { it.isSuccess() isBool true }

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.TEMPORARILY_RELEASED)
  }

  @Test
  fun `allocations are not deallocated on 'END' when prisoner not found`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber(any())) doReturn null

    val outcome = handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode))

    outcome.isSuccess() isBool false
    outcome.message isEqualTo "Unable to determine release reason for prisoner 123456"
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

  private fun scheduledInstanceOn(date: LocalDate, time: LocalTime): ScheduledInstance = mock {
    on { sessionDate } doReturn date
    on { startTime } doReturn time
  }

  private fun attendanceFor(instance: ScheduledInstance): Attendance = mock {
    on { scheduledInstance } doReturn instance
    on { editable() } doReturn true
  }

  @Test
  fun `released prisoner is deallocated on 'END'`() {
    mock<Prisoner> {
      on { status } doReturn "INACTIVE OUT"
      on { confirmedReleaseDate } doReturn TimeSource.today()
      on { lastMovementTypeCode } doReturn "REL"
    }.also { permanentlyReleasedPrisoner ->
      whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn permanentlyReleasedPrisoner
    }

    handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode))

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `released prisoner is not deallocated on 'END' when cannot determine release reason`() {
    mock<Prisoner> {
      on { status } doReturn "INACTIVE OUT"
      on { confirmedReleaseDate } doReturn TimeSource.today()
      on { lastMovementTypeCode } doReturn "XXX"
    }.also { permanentlyReleasedPrisoner ->
      whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn permanentlyReleasedPrisoner
    }

    assertThatThrownBy {
      handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode)).also { it.isSuccess() isBool false }
    }.isInstanceOf(IllegalStateException::class.java)

    verify(prisonerAllocationHandler, never()).deallocate(any(), any(), any())
  }

  @Test
  fun `no interactions when released prisoner has no allocations of interest`() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn false

    handler.handle(activitiesChangedEvent("123456", Action.END, moorlandPrisonCode)).also { it.isSuccess() isBool true }

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(prisonerAllocationHandler)
  }
}
