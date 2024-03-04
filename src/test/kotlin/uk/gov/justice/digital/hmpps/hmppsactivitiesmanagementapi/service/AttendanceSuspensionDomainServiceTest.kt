package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import java.time.LocalDate
import java.time.LocalDateTime

class AttendanceSuspensionDomainServiceTest {
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()

  private val service = AttendanceSuspensionDomainService(attendanceRepository, attendanceReasonRepository)

  @Nested
  inner class SuspendFutureAttendancesForAllocation {
    @BeforeEach
    fun setup() {
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(
        attendanceReasons()["SUSPENDED"],
      )

      val historicAttendance = Attendance(
        attendanceId = 1,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn false
          on { sessionDate } doReturn LocalDate.now().minusDays(1)
        },
        prisonerNumber = "123456",
      )

      val futureAttendance = Attendance(
        attendanceId = 2,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now().plusDays(1)
        },
        prisonerNumber = "123456",
      )

      whenever(
        attendanceRepository.findAttendancesOnOrAfterDateForAllocation(any(), any(), eq(AttendanceStatus.WAITING), any()),
      ).thenReturn(listOf(historicAttendance, futureAttendance))
    }

    @Test
    fun `supplied reason must be either SUSPENDED or AUTO_SUSPENDED`() {
      assertThatThrownBy {
        service.suspendFutureAttendancesForAllocation(AttendanceReasonEnum.CLASH, LocalDateTime.now(), allocation())
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Failed to suspend future attendances for allocation with id 0, because the provided attendance reason CLASH was not one of: SUSPENDED, AUTO_SUSPENDED")
    }

    @Test
    fun `only future attendances are suspended`() {
      service.suspendFutureAttendancesForAllocation(AttendanceReasonEnum.SUSPENDED, LocalDateTime.now(), allocation()).let { updatedAttendances ->
        updatedAttendances hasSize 1
        with(updatedAttendances.first()) {
          status() isEqualTo AttendanceStatus.COMPLETED
          issuePayment isEqualTo false
          attendanceReason isEqualTo attendanceReasons()["SUSPENDED"]
        }
      }
    }
  }

  @Nested
  inner class ResetFutureAttendancesForAllocation {
    @Test
    fun `supplied reason must be either SUSPENDED or AUTO_SUSPENDED`() {
      assertThatThrownBy {
        service.resetFutureSuspendedAttendancesForAllocation(AttendanceReasonEnum.CLASH, LocalDateTime.now(), allocation())
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Failed to suspend future attendances for allocation with id 0, because the provided attendance reason CLASH was not one of: SUSPENDED, AUTO_SUSPENDED")
    }

    @Test
    fun `future suspended attendances which get reset`() {
      val historicAttendance = Attendance(
        attendanceId = 1,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn false
          on { sessionDate } doReturn LocalDate.now().minusDays(1)
        },
        prisonerNumber = "123456",
      )

      val nonSuspended = Attendance(
        attendanceId = 2,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now()
        },
        attendanceReason = attendanceReasons()["ATTENDED"],
        prisonerNumber = "123456",
      )

      val suspendedAttendance = Attendance(
        attendanceId = 4,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now()
        },
        attendanceReason = attendanceReasons()["SUSPENDED"],
        prisonerNumber = "123456",
      )

      whenever(
        attendanceRepository.findAttendancesOnOrAfterDateForAllocation(any(), any(), eq(AttendanceStatus.COMPLETED), any()),
      ).thenReturn(listOf(historicAttendance, nonSuspended, suspendedAttendance))

      service.resetFutureSuspendedAttendancesForAllocation(AttendanceReasonEnum.SUSPENDED, LocalDateTime.now(), allocation()).let { updatedAttendances ->
        updatedAttendances hasSize 1
        with(updatedAttendances.first()) {
          status() isEqualTo AttendanceStatus.WAITING
          attendanceReason isEqualTo null
        }
      }
    }

    @Test
    fun `future auto-suspended attendances which get reset`() {
      val historicAttendance = Attendance(
        attendanceId = 1,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn false
          on { sessionDate } doReturn LocalDate.now().minusDays(1)
        },
        prisonerNumber = "123456",
      )

      val nonSuspended = Attendance(
        attendanceId = 2,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now()
        },
        attendanceReason = attendanceReasons()["ATTENDED"],
        prisonerNumber = "123456",
      )

      val suspendedAttendance = Attendance(
        attendanceId = 4,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now()
        },
        attendanceReason = attendanceReasons()["AUTO_SUSPENDED"],
        prisonerNumber = "123456",
      )

      whenever(
        attendanceRepository.findAttendancesOnOrAfterDateForAllocation(any(), any(), eq(AttendanceStatus.COMPLETED), any()),
      ).thenReturn(listOf(historicAttendance, nonSuspended, suspendedAttendance))

      service.resetFutureSuspendedAttendancesForAllocation(AttendanceReasonEnum.AUTO_SUSPENDED, LocalDateTime.now(), allocation()).let { updatedAttendances ->
        updatedAttendances hasSize 1
        with(updatedAttendances.first()) {
          status() isEqualTo AttendanceStatus.WAITING
          attendanceReason isEqualTo null
        }
      }
    }

    @Test
    fun `cancelled sessions have their suspended attendances reset to cancelled`() {
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(
        attendanceReasons()["CANCELLED"],
      )

      val cancelledAttendance = Attendance(
        attendanceId = 3,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now()
          on { cancelled } doReturn true
        },
        attendanceReason = attendanceReasons()["SUSPENDED"],
        prisonerNumber = "123456",
      )

      whenever(
        attendanceRepository.findAttendancesOnOrAfterDateForAllocation(any(), any(), eq(AttendanceStatus.COMPLETED), any()),
      ).thenReturn(listOf(cancelledAttendance))

      service.resetFutureSuspendedAttendancesForAllocation(AttendanceReasonEnum.SUSPENDED, LocalDateTime.now(), allocation()).let { updatedAttendances ->
        updatedAttendances hasSize 1
        with(updatedAttendances.first()) {
          status() isEqualTo AttendanceStatus.COMPLETED
          attendanceReason isEqualTo attendanceReasons()["CANCELLED"]
        }
      }
    }

    @Test
    fun `auto-suspended attendances get reset to suspended if the allocation has a planned suspension`() {
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(
        attendanceReasons()["SUSPENDED"],
      )

      val autoSuspendedAttendance = Attendance(
        attendanceId = 3,
        status = AttendanceStatus.COMPLETED,
        scheduledInstance = mock {
          on { isFuture(any()) } doReturn true
          on { sessionDate } doReturn LocalDate.now()
          on { cancelled } doReturn false
        },
        attendanceReason = attendanceReasons()["AUTO_SUSPENDED"],
        prisonerNumber = "123456",
      )

      whenever(
        attendanceRepository.findAttendancesOnOrAfterDateForAllocation(any(), any(), eq(AttendanceStatus.COMPLETED), any()),
      ).thenReturn(listOf(autoSuspendedAttendance))

      service.resetFutureSuspendedAttendancesForAllocation(AttendanceReasonEnum.AUTO_SUSPENDED, LocalDateTime.now(), allocation(withPlannedSuspensions = true)).let { updatedAttendances ->
        updatedAttendances hasSize 1
        with(updatedAttendances.first()) {
          status() isEqualTo AttendanceStatus.COMPLETED
          attendanceReason isEqualTo attendanceReasons()["SUSPENDED"]
        }
      }
    }
  }
}
