package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
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
  private val activitySchedule = activity.schedules.first()
  private val allocation = activitySchedule.allocations.first()
  private val instance = activitySchedule.instances.first()
  private val today = LocalDate.now()

  @Test
  fun `attendance record is created when no pre-existig attendance record`() {
    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.createAttendanceRecordsFor(today)

    verify(attendanceRepository).save(
      Attendance(
        scheduledInstance = instance,
        prisonerNumber = instance.activitySchedule.allocations.first().prisonerNumber,
        posted = false
      )
    )
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
}
