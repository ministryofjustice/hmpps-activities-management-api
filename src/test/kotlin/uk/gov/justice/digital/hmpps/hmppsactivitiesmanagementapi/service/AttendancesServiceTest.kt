package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate

class AttendancesServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val service =
    AttendancesService(scheduledInstanceRepository, attendanceRepository, attendanceReasonRepository)
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val allocation = activitySchedule.allocations().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()
  private val today = LocalDate.now()
  private val tomorrow = today.plusDays(1)

  @Test
  fun `attendance record is created when no pre-existing attendance record and attendance is required`() {
    instance.activitySchedule.activity.attendanceRequired = true

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.createAttendanceRecordsFor(today)

    verify(attendanceRepository).save(
      Attendance(
        scheduledInstance = instance,
        prisonerNumber = instance.activitySchedule.allocations().first().prisonerNumber,
        posted = false,
        status = AttendanceStatus.SCHEDULED
      )
    )
  }

  @Test
  fun `attendance record is not created when no pre-existing attendance record and attendance is not required`() {
    instance.activitySchedule.activity.attendanceRequired = false

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.createAttendanceRecordsFor(today)

    verify(attendanceRepository, never()).save(any())
  }

  @Test
  fun `attendance record is not created if pre-existing attendance record`() {
    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(
      attendanceRepository.existsAttendanceByScheduledInstanceAndPrisonerNumber(
        instance,
        allocation.prisonerNumber
      )
    ).thenReturn(true)

    service.createAttendanceRecordsFor(today)

    verify(attendanceRepository, never()).save(any())
  }

  @Test
  fun `attendance record is not created today if allocation is active from tomorrow`() {
    allocation.starts(tomorrow)

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.createAttendanceRecordsFor(today)

    verify(attendanceRepository, never()).existsAttendanceByScheduledInstanceAndPrisonerNumber(any(), any())
    verify(attendanceRepository, never()).save(any())
  }

  @Test
  fun `mark attendance record`() {
    assertThat(attendance.status).isEqualTo(AttendanceStatus.SCHEDULED)
    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark(listOf(AttendanceUpdateRequest(attendance.attendanceId, "ATT")))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATT"])
  }

  private fun Allocation.starts(date: LocalDate) {
    startDate = date
  }
}
