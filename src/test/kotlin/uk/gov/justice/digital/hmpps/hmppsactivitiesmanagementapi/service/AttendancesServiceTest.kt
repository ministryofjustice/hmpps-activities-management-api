package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendanceSummary as ModelAllAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

class AttendancesServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val allAttendanceSummaryRepository: AllAttendanceSummaryRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock()
  private val service =
    AttendancesService(
      scheduledInstanceRepository,
      allAttendanceSummaryRepository,
      attendanceRepository,
      attendanceReasonRepository,
      caseNotesApiClient,
    )
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val allocation = activitySchedule.allocations().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()
  private val today = LocalDate.now()

  private val attendanceCaptor = argumentCaptor<Attendance>()

  @Test
  fun `attendance record is created when no pre-existing attendance record, attendance is required and allocation active`() {
    instance.activitySchedule.activity.attendanceRequired = true

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))

    service.createAttendanceRecordsFor(today)

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

    service.createAttendanceRecordsFor(today)

    verifyNoInteractions(attendanceRepository)
  }

  @Test
  fun `attendance record is created and marked as not attended when allocation is auto suspended`() {
    instance.activitySchedule.activity.attendanceRequired = true
    allocation.autoSuspend(today.atStartOfDay(), "reason")

    whenever(scheduledInstanceRepository.findAllBySessionDate(today)).thenReturn(listOf(instance))
    whenever(attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)).thenReturn(attendanceReasons()["SUSPENDED"])

    service.createAttendanceRecordsFor(today)

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

    service.createAttendanceRecordsFor(today)

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

    service.createAttendanceRecordsFor(today)

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

    service.createAttendanceRecordsFor(today)

    verify(attendanceRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `mark attendance record`() {
    assertThat(attendance.status).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, moorlandPrisonCode, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null)))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
  }

  @Test
  fun `remove attendance`() {
    attendance.status = AttendanceStatus.COMPLETED
    attendance.attendanceReason = AttendanceReason(
      9,
      AttendanceReasonEnum.ATTENDED,
      "Attended",
      false,
      true,
      true,
      false,
      false,
      false,
      true,
      1,
      "some note",
    )
    attendance.issuePayment = true

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, moorlandPrisonCode, AttendanceStatus.WAITING, null, null, null, null, null, null)))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status).isEqualTo(AttendanceStatus.WAITING)
    assertThat(attendance.attendanceReason).isNull()
    assertThat(attendance.issuePayment).isNull()
    assertThat(attendance.payAmount).isNull()
  }

  private fun Allocation.starts(date: LocalDate) {
    startDate = date
  }

  @Test
  fun `success`() {
    whenever(attendanceRepository.findById(1)).thenReturn(
      Optional.of(
        attendance(),
      ),
    )
    assertThat(service.getAttendanceById(1)).isInstanceOf(ModelAttendance::class.java)
  }

  @Test
  fun `not found`() {
    whenever(attendanceRepository.findById(1)).thenReturn(Optional.empty())
    Assertions.assertThatThrownBy { service.getAttendanceById(-1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Attendance -1 not found")
  }

  @Test
  fun `retrieve attendance summary`() {
    whenever(allAttendanceSummaryRepository.findByPrisonCodeAndSessionDate(pentonvillePrisonCode, LocalDate.now())).thenReturn(
      attendanceSummary(),
    )
    assertThat(service.getAttendanceSummaryByDate(pentonvillePrisonCode, LocalDate.now()).first()).isInstanceOf(ModelAllAttendanceSummary::class.java)
  }
}
