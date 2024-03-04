package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

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
    @BeforeEach
    fun setup() {
      whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)).thenReturn(
        attendanceReasons()["CANCELLED"],
      )

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
      ).thenReturn(listOf(historicAttendance, nonSuspended, cancelledAttendance, suspendedAttendance))
    }

    @Test
    fun `future attendances which are suspended get reset`() {
      service.resetFutureSuspendedAttendancesForAllocation(AttendanceReasonEnum.SUSPENDED, LocalDateTime.now(), allocation()).let { updatedAttendances ->
        updatedAttendances hasSize 2
        with(updatedAttendances.first()) {
          status() isEqualTo AttendanceStatus.COMPLETED
          attendanceReason isEqualTo attendanceReasons()["CANCELLED"]
        }
        with(updatedAttendances.last()) {
          status() isEqualTo AttendanceStatus.WAITING
          attendanceReason isEqualTo null
        }
      }
    }
  }
}
