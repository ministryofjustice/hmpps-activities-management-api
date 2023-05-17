package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ManageAttendancesServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val service =
    ManageAttendancesService(scheduledInstanceRepository, attendanceRepository, attendanceReasonRepository)
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val allocation = activitySchedule.allocations().first()
  private val instance = activitySchedule.instances().first()
  private val today = LocalDate.now()

  private val attendanceCaptor = argumentCaptor<Attendance>()

  @Test
  fun `attendance record is created when no pre-existing attendance record, attendance is required and allocation active`() {
    instance.activitySchedule.activity.attendanceRequired = true

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(
      Attendance(
        scheduledInstance = instance,
        prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
        status = AttendanceStatus.WAITING,
        payAmount = 30,
      ),
    )
  }

  @Test
  fun `attendance record is not created when allocation has ended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.deallocate(today.atStartOfDay(), "reason")

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.attendances(AttendanceOperation.CREATE)

    verifyNoInteractions(attendanceRepository)
  }

  @Test
  fun `attendance record is created and marked as not attended when allocation is auto suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.autoSuspend(today.atStartOfDay(), "reason")

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture())
    with(attendanceCaptor.firstValue) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status).isEqualTo(AttendanceStatus.COMPLETED)
    }
  }

  @Test
  fun `attendance record is created and marked as not attended when allocation is suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.userSuspend(today.atStartOfDay(), "reason", "user")

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture())
    with(attendanceCaptor.firstValue) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(issuePayment).isFalse
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(status).isEqualTo(AttendanceStatus.COMPLETED)
    }
  }

  @Test
  fun `attendance record is not created when no pre-existing attendance record and attendance is not required`() {
    instance.activitySchedule.activity.attendanceRequired = false

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `attendance record is not created if pre-existing attendance record`() {
    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(
      attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(
        instance,
        allocation.prisonerNumber,
      ),
    ).thenReturn(true)

    service.attendances(AttendanceOperation.CREATE)

    verify(attendanceRepository, never()).saveAndFlush(any())
  }
}
