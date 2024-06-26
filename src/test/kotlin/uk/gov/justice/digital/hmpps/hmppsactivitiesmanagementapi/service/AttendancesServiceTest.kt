package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.SuspendedPrisonerAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as ModelAllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

class AttendancesServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val allAttendanceRepository: AllAttendanceRepository = mock()
  private val attendanceReasonRepository: AttendanceReasonRepository = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val service =
    AttendancesService(
      scheduledInstanceRepository,
      allAttendanceRepository,
      attendanceRepository,
      attendanceReasonRepository,
      caseNotesApiClient,
      TransactionHandler(),
      outboundEventsService,
      telemetryClient,
    )
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()

  @Nested
  inner class SuspendedPrisonerAttendanceTest {
    val localTime = LocalTime.now()

    inner class TestData : SuspendedPrisonerAttendance {
      override fun getPrisonerNumber(): String = "prisoner"
      override fun getStartTime(): LocalTime = localTime
      override fun getEndTime(): LocalTime = localTime.plusHours(1)
      override fun getCategoryName(): String = "CAT"
      override fun getAttendanceReasonCode(): String = "REASON"
      override fun getTimeSlot(): String = "TIME"
      override fun getInCell(): Boolean = false
      override fun getOffWing(): Boolean = false
      override fun getOnWing(): Boolean = false
      override fun getInternalLocation(): String? = "desc"
    }

    @Test
    fun `maps fields for UI correctly`() {
      whenever(
        attendanceRepository.getSuspendedPrisonerAttendance(
          prisonCode = "MDI",
          date = LocalDate.now(),
        ),
      ).thenReturn(
        listOf(
          TestData(),
        ),
      )

      val response = service.getSuspendedPrisonerAttendance(
        prisonCode = "MDI",
        date = LocalDate.now(),
      ).first()

      assertThat(response.prisonerNumber).isEqualTo("prisoner")
      assertThat(response.attendance.first().startTime).isEqualTo(localTime)
      assertThat(response.attendance.first().endTime).isEqualTo(localTime.plusHours(1))
      assertThat(response.attendance.first().categoryName).isEqualTo("CAT")
      assertThat(response.attendance.first().attendanceReasonCode).isEqualTo("REASON")
      assertThat(response.attendance.first().timeSlot).isEqualTo("TIME")
      assertThat(response.attendance.first().inCell).isFalse()
      assertThat(response.attendance.first().onWing).isFalse()
      assertThat(response.attendance.first().offWing).isFalse()
      assertThat(response.attendance.first().internalLocation).isEqualTo("desc")
    }
  }

  @Test
  fun `mark attendance record`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null)))

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
    verify(telemetryClient).trackEvent(
      TelemetryEvent.RECORD_ATTENDANCE.value,
      attendance.toTelemetryPropertiesMap(),
    )
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
  }

  @Test
  fun `mark attendance record with case note - Refused to attend`() {
    val expectedCaseNotePrefix = "Refused to attend - Maths - Education - R1 - ${LocalDate.now().atTime(attendance.scheduledInstance.startTime).toMediumFormatStyle()}"

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(CaseNoteType.NEG), eq(CaseNoteSubType.NEG_GEN), eq(expectedCaseNotePrefix))).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "REFUSED", null, null, "test case note", null, null)))

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["REFUSED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isNull()
  }

  @Test
  fun `mark attendance record with case note - Pay removed`() {
    val expectedCaseNotePrefix = "Pay removed - Maths - Education - R1 - ${LocalDate.now().atTime(attendance.scheduledInstance.startTime).toMediumFormatStyle()}"

    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance.apply { issuePayment = true }))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(CaseNoteType.NEG), eq(CaseNoteSubType.NEG_GEN), eq(expectedCaseNotePrefix))).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, false, "test case note", null, null)))

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
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
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(CaseNoteType.NEG), eq(CaseNoteSubType.NEG_GEN), any())).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "REFUSED", null, null, "test case note", false, null)))

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["REFUSED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isFalse
  }

  @Test
  fun `mark attendance record with case note and incentive level warning issued`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)
    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(CaseNoteType.NEG), eq(CaseNoteSubType.IEP_WARN), any())).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "REFUSED", null, null, "test case note", true, null)))

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["REFUSED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isTrue
  }

  @Test
  fun `mark attendance record - case note not prefixed if the person did attend`() {
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.WAITING)

    assertThat(attendance.attendanceReason).isNull()

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(attendance.attendanceId))).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.postCaseNote(any(), any(), any(), eq(CaseNoteType.NEG), eq(CaseNoteSubType.NEG_GEN), eq(null))).thenReturn(caseNote)

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(attendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, "test case note", null, null)))

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
    assertThat(attendance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendance.attendanceReason).isEqualTo(attendanceReasons()["ATTENDED"])
    assertThat(attendance.caseNoteId).isEqualTo(1)
    assertThat(attendance.incentiveLevelWarningIssued).isNull()
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
          MOORLAND_PRISON_CODE,
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

    verify(attendanceRepository).saveAndFlush(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
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
      initialIssuePayment = false,
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
        initialIssuePayment = true,
        attendanceReason = attendanceReasons()["ATTENDED"]!!,
        status = AttendanceStatus.COMPLETED,
      ),
    )

    assertThat(attendances.filter { it.attendanceReason == attendanceReasons()["ATTENDED"] }.size).isEqualTo(1)

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(attendances.map { it.attendanceId }.toSet())).thenReturn(attendances)

    assertThatThrownBy {
      service.mark(
        "Joe Bloggs",
        attendances.map {
          AttendanceUpdateRequest(
            it.attendanceId,
            MOORLAND_PRISON_CODE,
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

    verify(attendanceRepository, never()).saveAllAndFlush(any<List<Attendance>>())
    verify(outboundEventsService, never()).send(any(), any())
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
          MOORLAND_PRISON_CODE,
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

    verify(attendanceRepository).saveAndFlush(attendance)
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

    service.mark("Joe Bloggs", listOf(AttendanceUpdateRequest(completedAttendance.attendanceId, MOORLAND_PRISON_CODE, AttendanceStatus.WAITING, null, null, null, null, null, null)))

    verify(attendanceRepository).saveAndFlush(completedAttendance)

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
    assertThatThrownBy { service.getAttendanceById(-1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Attendance -1 not found")
  }

  @Test
  fun `retrieve daily attendance list`() {
    whenever(allAttendanceRepository.findByPrisonCodeAndSessionDate(PENTONVILLE_PRISON_CODE, LocalDate.now())).thenReturn(
      attendanceList(),
    )
    assertThat(service.getAllAttendanceByDate(PENTONVILLE_PRISON_CODE, LocalDate.now()).first()).isInstanceOf(ModelAllAttendance::class.java)
  }

  @Test
  fun `marking attendance records to issue payment is ignored for unpaid activity`() {
    val unpaidSession = activitySchedule(activityEntity(paid = false, noPayBands = true), paid = false).instances().first()

    val unpaidAttendance = attendance.copy(
      recordedTime = null,
      initialIssuePayment = null,
      attendanceReason = null,
      status = AttendanceStatus.WAITING,
      scheduledInstance = unpaidSession,
    )

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(unpaidAttendance.attendanceId))).thenReturn(listOf(unpaidAttendance))

    service.mark(
      "Joe Bloggs",
      listOf(unpaidAttendance).map {
        AttendanceUpdateRequest(
          it.attendanceId,
          MOORLAND_PRISON_CODE,
          AttendanceStatus.COMPLETED,
          "ATTENDED",
          null,
          true,
          null,
          null,
          null,
        )
      },
    )

    unpaidAttendance.issuePayment!! isBool false
  }

  @Test
  fun `marking attendance records to issue payment is not ignored for paid activity`() {
    val paidSession = activitySchedule(activityEntity(), paid = true).instances().first()

    val paidAttendance = paidSession.attendances.first().copy(
      recordedTime = null,
      initialIssuePayment = null,
      attendanceReason = null,
      status = AttendanceStatus.WAITING,
      scheduledInstance = paidSession,
    )

    whenever(attendanceReasonRepository.findAll()).thenReturn(attendanceReasons().map { it.value })
    whenever(attendanceRepository.findAllById(setOf(paidAttendance.attendanceId))).thenReturn(listOf(paidAttendance))

    service.mark(
      "Joe Bloggs",
      listOf(paidAttendance).map {
        AttendanceUpdateRequest(
          it.attendanceId,
          MOORLAND_PRISON_CODE,
          AttendanceStatus.COMPLETED,
          "ATTENDED",
          null,
          true,
          null,
          null,
          null,
        )
      },
    )

    paidAttendance.issuePayment!! isBool true
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
