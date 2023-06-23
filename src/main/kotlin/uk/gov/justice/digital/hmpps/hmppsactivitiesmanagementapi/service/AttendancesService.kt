package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as ModelAllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendanceSummary as ModelAllAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@Service
@Transactional(readOnly = true)
class AttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val allAttendanceRepository: AllAttendanceRepository,
  private val allAttendanceSummaryRepository: AllAttendanceSummaryRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val caseNotesApiClient: CaseNotesApiClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findAttendancesByScheduledInstance(instanceId: Long) =
    scheduledInstanceRepository.findOrThrowNotFound(instanceId).attendances.map { transform(it, null) }

  // TODO this is a very thin slice when updating.
  // TODO some of the attributes still need populating as part of the marking journey e.g. recorded time/by, pay etc.
  // TODO also there is no validation checking.
  @Transactional
  @PreAuthorize("hasAnyRole('ACTIVITY_ADMIN')")
  fun mark(principalName: String, attendances: List<AttendanceUpdateRequest>) {
    val attendanceUpdatesById = attendances.associateBy { it.id }
    val attendanceReasonsByCode = attendanceReasonRepository.findAll().associateBy { it.code.toString() }

    val updatedAttendances = attendanceRepository.findAllById(attendanceUpdatesById.keys).mapNotNull {
      var caseNoteDetails: CaseNote? = null
      if (!attendanceUpdatesById.containsKey(it.attendanceId)) { throw IllegalArgumentException("Attendance record not found") }
      if (!attendanceUpdatesById[it.attendanceId]!!.caseNote.isNullOrEmpty()) {
        caseNoteDetails = caseNotesApiClient.postCaseNote(attendanceUpdatesById[it.attendanceId]!!.prisonCode, it.prisonerNumber, attendanceUpdatesById[it.attendanceId]!!.caseNote!!, attendanceUpdatesById[it.attendanceId]!!.incentiveLevelWarningIssued)
      }
      it.mark(
        principalName,
        attendanceReasonsByCode[attendanceUpdatesById[it.attendanceId]!!.attendanceReason?.uppercase()?.trim()],
        attendanceUpdatesById[it.attendanceId]?.status ?: AttendanceStatus.COMPLETED,
        attendanceUpdatesById[it.attendanceId]!!.comment,
        attendanceUpdatesById[it.attendanceId]!!.issuePayment,
        attendanceUpdatesById[it.attendanceId]!!.incentiveLevelWarningIssued,
        caseNoteDetails?.caseNoteId,
        attendanceUpdatesById[it.attendanceId]!!.otherAbsenceReason,
      )
    }

    attendanceRepository.saveAll(updatedAttendances)
  }

  fun getAttendanceById(id: Long): ModelAttendance {
    val attendance = attendanceRepository.findOrThrowNotFound(id)
    return transform(
      attendance,
      attendance.caseNoteId?.let { caseNotesApiClient.getCaseNote(attendance.prisonerNumber, attendance.caseNoteId)?.text },
    )
  }

  fun getAttendanceSummaryByDate(prisonCode: String, sessionDate: LocalDate): List<ModelAllAttendanceSummary> =
    allAttendanceSummaryRepository.findByPrisonCodeAndSessionDate(prisonCode, sessionDate).toModel()

  fun getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate): List<ModelAllAttendance> =
    allAttendanceRepository.findByPrisonCodeAndSessionDate(prisonCode, sessionDate).toModel()
}
