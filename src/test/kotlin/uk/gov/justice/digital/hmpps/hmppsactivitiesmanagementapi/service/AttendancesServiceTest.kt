package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as ModelAllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendanceSummary as ModelAllAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

class AttendancesServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val allAttendanceRepository: AllAttendanceRepository = mock()
  private val allAttendanceSummaryRepository: AllAttendanceSummaryRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock()
  private val service =
    AttendancesService(
      scheduledInstanceRepository,
      allAttendanceRepository,
      allAttendanceSummaryRepository,
      attendanceRepository,
      attendanceReasonRepository,
      caseNotesApiClient,
    )
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()

  @Test
  fun `mark attendance record`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, moorlandPrisonCode, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null)))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
  }

  @Test
  fun `mark attendance record with case note`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(null))).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, moorlandPrisonCode, AttendanceStatus.COMPLETED, "ATTENDED", null, null, "test case note", null, null)))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isNull()
  }

  @Test
  fun `mark attendance record with case note and incentive level warning not issued`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(false))).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, moorlandPrisonCode, AttendanceStatus.COMPLETED, "ATTENDED", null, null, "test case note", false, null)))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isFalse
  }

  @Test
  fun `mark attendance record with case note and incentive level warning issued`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(true))).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, moorlandPrisonCode, AttendanceStatus.COMPLETED, "ATTENDED", null, null, "test case note", true, null)))

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isTrue
  }

  @Test
  fun `mark attendance record with other absence reason`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    assertThat(attendance.attendanceReason).isNull()
    assertThat(attendance.otherAbsenceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark(
      "Joe Bloggs",
      listOf(
        AttendanceUpdateRequest(
          attendance.attendanceId,
          moorlandPrisonCode,
          AttendanceStatus.COMPLETED,
          "OTHER",
          null,
          null,
          "test case note",
          true,
          "other absence reason",
        ),
      ),
    )

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    with(attendance.attendanceReason!!) {
      assertThat(code).isEqualTo(AttendanceReasonEnum.OTHER)
    }
    assertThat(attendance.otherAbsenceReason).isEqualTo("other absence reason")
  }

  @Test
  fun `marking attendance records should fail if one is not editable`() {
    val pastInstance = instance.copy(
      sessionDate = LocalDate.now().minusDays(1),
    )
    val waitingAttendance = attendance.copy(
      recordedTime = null,
      issuePayment = false,
      attendanceReason = null,
      status = AttendanceStatus.WAITING,
      scheduledInstance = pastInstance,
    )

    val attendances = listOf(
      waitingAttendance,
      waitingAttendance.copy(attendanceId = 2, prisonerNumber = "B2222BB"),
      waitingAttendance.copy(attendanceId = 3, prisonerNumber = "C3333CC"),
      waitingAttendance.copy(
        attendanceId = 4,
        prisonerNumber = "D4444DD",
        recordedTime = LocalDateTime.now().minusDays(2),
        issuePayment = true,
        attendanceReason = attendanceReasons()["ATTENDED"]!!,
        status = AttendanceStatus.COMPLETED,
      ),
    )

    assertThat(attendances.filter { it.attendanceReason == attendanceReasons()["ATTENDED"] }.size).isEqualTo(1)

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(attendances.map { it.attendanceId }.toSet())).thenReturn(attendances)

    Assertions.assertThatThrownBy {
      service.mark(
        "Joe Bloggs",
        attendances.map {
          AttendanceUpdateRequest(
            it.attendanceId,
            moorlandPrisonCode,
            AttendanceStatus.COMPLETED,
            "ATTENDED",
            null,
            null,
            null,
            null,
            null,
          )
        },
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance record for prisoner 'D4444DD' can no longer be modified")

    verify(attendanceRepository, never()).saveAll(any<List<Attendance>>())
  }

  @Test
  fun `other absence reason is not set when attendance reason does not equal OTHER`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    assertThat(attendance.attendanceReason).isNull()
    assertThat(attendance.otherAbsenceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark(
      "Joe Bloggs",
      listOf(
        AttendanceUpdateRequest(
          attendance.attendanceId,
          moorlandPrisonCode,
          AttendanceStatus.COMPLETED,
          "REFUSED",
          null,
          null,
          "test case note",
          true,
          "other absence reason",
        ),
      ),
    )

    verify(attendanceRepository).saveAll(listOf(attendance))
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    with(attendance.attendanceReason!!) {
      assertThat(code).isEqualTo(AttendanceReasonEnum.REFUSED)
    }
    assertThat(attendance.otherAbsenceReason).isNull()
  }

  @Test
  fun `remove attendance`() {
    val completedAttendance = attendance.copy(status = AttendanceStatus.COMPLETED)

    completedAttendance.attendanceReason = AttendanceReason(
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
    completedAttendance.issuePayment = true

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(completedAttendance.attendanceId))).thenReturn(listOf(completedAttendance))

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(completedAttendance.attendanceId, moorlandPrisonCode, AttendanceStatus.WAITING, null, null, null, null, null, null)))

    verify(attendanceRepository).saveAll(listOf(completedAttendance))

    with(completedAttendance) {
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(attendanceReason).isNull()
      assertThat(issuePayment).isNull()
      assertThat(payAmount).isNull()
    }
  }

  private fun Allocation.starts(date: LocalDate) {
    startDate = date
  }

  @Test
  fun `successful attendance transformation`() {
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

  @Test
  fun `retrieve daily attendance list`() {
    whenever(allAttendanceRepository.findByPrisonCodeAndSessionDate(pentonvillePrisonCode, LocalDate.now())).thenReturn(
      attendanceList(),
    )
    assertThat(service.getAllAttendanceByDate(pentonvillePrisonCode, LocalDate.now()).first()).isInstanceOf(ModelAllAttendance::class.java)
  }

  companion object {
    val caseNote = CaseNote(
      caseNoteId = "1",
      offenderIdentifier = "A1234AA",
      type = "NEG",
      typeDescription = "Negative",
      subType = "sub type",
      subTypeDescription = "sub type description",
      source = "source",
      creationDateTime = LocalDateTime.now(),
      occurrenceDateTime = LocalDateTime.now(),
      authorName = "author",
      authorUserId = "author id",
      text = "Case Note Text",
      eventId = 1,
      sensitive = false,
    )
  }
}
