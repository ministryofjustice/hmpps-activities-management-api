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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class OffenderReceivedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val handler =
    OffenderReceivedEventHandler(rolloutPrisonRepository, allocationRepository, prisonerSearchApiClient, attendanceRepository, attendanceReasonRepository, TransactionHandler(), outboundEventsService)

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
      on { findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456") } doReturn allocations
    }

    val outcome = handler.handle(offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456"))

    assertThat(outcome.isSuccess()).isTrue
    assertThat(autoSuspendedOne.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(autoSuspendedTwo.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(userSuspended.status(PrisonerStatus.SUSPENDED)).isTrue
    assertThat(ended.status(PrisonerStatus.ENDED)).isTrue
  }

  @Test
  fun `future attendances are unsuspended on receipt of prisoner`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.AUTO_SUSPENDED)).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")) doReturn it
    }

    val historicAttendance = Attendance(
      scheduledInstance = mock {
        on { sessionDate } doReturn TimeSource.today()
        on { startTime } doReturn LocalTime.now().minusMinutes(1)
      },
      prisonerNumber = "123456",
    ).completeWithoutPayment(
      mock {
        on { code } doReturn AttendanceReasonEnum.SUSPENDED
      },
    )

    val todaysFutureAttendance = Attendance(
      scheduledInstance = mock {
        on { startTime } doReturn LocalTime.now().plusMinutes(1)
        on { sessionDate } doReturn TimeSource.today()
      },
      prisonerNumber = "123456",
    ).completeWithoutPayment(
      mock {
        on { code } doReturn AttendanceReasonEnum.SUSPENDED
      },
    )

    // Technically we do not create attendances beyond today. This is included to future-proof should that rule change.
    val tomorrowsFutureAttendance = Attendance(
      scheduledInstance = mock {
        on { startTime } doReturn LocalTime.now().plusMinutes(1)
        on { sessionDate } doReturn TimeSource.tomorrow()
      },
      prisonerNumber = "123456",
    ).completeWithoutPayment(
      mock {
        on { code } doReturn AttendanceReasonEnum.SUSPENDED
      },
    )

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        moorlandPrisonCode,
        LocalDate.now(),
        AttendanceStatus.COMPLETED,
        "123456",
      ),
    ) doReturn listOf(historicAttendance, todaysFutureAttendance, tomorrowsFutureAttendance)

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn activeInMoorlandPrisoner.copy(prisonerNumber = "123456")
    }

    handler.handle(offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456"))

    assertThat(historicAttendance.hasReason(AttendanceReasonEnum.SUSPENDED)).isTrue
    assertThat(todaysFutureAttendance.hasReason(AttendanceReasonEnum.SUSPENDED)).isFalse
    assertThat(tomorrowsFutureAttendance.hasReason(AttendanceReasonEnum.SUSPENDED)).isFalse
    verify(outboundEventsService, times(2)).send(eq(OutboundEvent.PRISONER_ATTENDANCE_AMENDED), any())
  }

  @Test
  fun `future attendances are cancelled on receipt of prisoner when instance cancelled after initial suspension`() {
    listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456", prisonerStatus = PrisonerStatus.AUTO_SUSPENDED)).also {
      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, "123456")) doReturn it
    }

    val cancelledAttendanceReason = mock<AttendanceReason>().also {
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)) doReturn it
      whenever(it.code) doReturn AttendanceReasonEnum.CANCELLED
    }

    val historicAttendance = Attendance(
      scheduledInstance = mock {
        on { sessionDate } doReturn TimeSource.today()
        on { startTime } doReturn LocalTime.now().minusMinutes(1)
        on { cancelled } doReturn true
      },
      prisonerNumber = "123456",
    ).completeWithoutPayment(
      mock {
        on { code } doReturn AttendanceReasonEnum.SUSPENDED
      },
    )

    val todaysFutureAttendance = Attendance(
      scheduledInstance = mock {
        on { startTime } doReturn LocalTime.now().plusMinutes(1)
        on { sessionDate } doReturn TimeSource.today()
        on { cancelled } doReturn true
      },
      prisonerNumber = "123456",
    ).completeWithoutPayment(
      mock {
        on { code } doReturn AttendanceReasonEnum.SUSPENDED
      },
    )

    // Technically we do not create attendances beyond today. This is included to future-proof should that rule change.
    val tomorrowsFutureAttendance = Attendance(
      scheduledInstance = mock {
        on { startTime } doReturn LocalTime.now().plusMinutes(1)
        on { sessionDate } doReturn TimeSource.tomorrow()
        on { cancelled } doReturn true
      },
      prisonerNumber = "123456",
    ).completeWithoutPayment(
      mock {
        on { code } doReturn AttendanceReasonEnum.SUSPENDED
      },
    )

    whenever(
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        moorlandPrisonCode,
        LocalDate.now(),
        AttendanceStatus.COMPLETED,
        "123456",
      ),
    ) doReturn listOf(historicAttendance, todaysFutureAttendance, tomorrowsFutureAttendance)

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn activeInMoorlandPrisoner.copy(prisonerNumber = "123456")
    }

    handler.handle(offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456"))

    assertThat(historicAttendance.hasReason(AttendanceReasonEnum.SUSPENDED)).isTrue
    assertThat(todaysFutureAttendance.hasReason(AttendanceReasonEnum.CANCELLED)).isTrue
    assertThat(todaysFutureAttendance.attendanceReason).isEqualTo(cancelledAttendanceReason)
    assertThat(tomorrowsFutureAttendance.hasReason(AttendanceReasonEnum.CANCELLED)).isTrue
    assertThat(tomorrowsFutureAttendance.attendanceReason).isEqualTo(cancelledAttendanceReason)
  }
}
