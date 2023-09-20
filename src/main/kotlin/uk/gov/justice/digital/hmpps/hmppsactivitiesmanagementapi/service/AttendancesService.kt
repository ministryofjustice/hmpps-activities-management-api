package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as ModelAllAttendance

@Service
class AttendancesService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val allAttendanceRepository: AllAttendanceRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val caseNotesApiClient: CaseNotesApiClient,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findAttendancesByScheduledInstance(instanceId: Long) =
    scheduledInstanceRepository.findOrThrowNotFound(instanceId).attendances.map { transform(it, null) }

  fun mark(principalName: String, attendances: List<AttendanceUpdateRequest>) {
    log.info("Attendance marking in progress")

    val markedAttendanceIds = transactionHandler.new {
      val attendanceUpdatesById = attendances.onEach(AttendanceUpdateRequestValidator::validate).associateBy { it.id }
      val attendanceReasonsByCode = attendanceReasonRepository.findAll().associateBy { it.code }

      attendanceRepository.findAllById(attendanceUpdatesById.keys).onEach { attendance ->
        val updateRequest = attendanceUpdatesById[attendance.attendanceId]!!

        attendance.mark(
          principalName = principalName,
          reason = attendanceReasonsByCode[updateRequest.maybeAttendanceReason()],
          newStatus = updateRequest.status,
          newComment = updateRequest.comment,
          newIssuePayment = updateRequest.issuePayment,
          newIncentiveLevelWarningIssued = updateRequest.incentiveLevelWarningIssued,
          newCaseNoteId = updateRequest.mayBeCaseNote(attendance.prisonerNumber)?.caseNoteId,
          newOtherAbsenceReason = updateRequest.otherAbsenceReason,
        )

        attendanceRepository.saveAndFlush(attendance)
      }.map { it.attendanceId }
    }.getOrThrow()

    markedAttendanceIds.forEach { id ->
      outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, id)
    }.also { log.info("Sending attendance amended events.") }

    log.info("Attendance marking done for ${markedAttendanceIds.size} attendance record(s)")
  }

  private fun AttendanceUpdateRequest.mayBeCaseNote(prisonerNumber: String): CaseNote? =
    caseNote?.let {
      caseNotesApiClient.postCaseNote(prisonCode, prisonerNumber, caseNote, incentiveLevelWarningIssued)
    }

  private fun AttendanceUpdateRequest.maybeAttendanceReason() =
    attendanceReason?.let { AttendanceReasonEnum.valueOf(it.trim().uppercase()) }

  fun getAttendanceById(id: Long) =
    transform(attendanceRepository.findOrThrowNotFound(id), caseNotesApiClient)

  fun getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate): List<ModelAllAttendance> =
    allAttendanceRepository.findByPrisonCodeAndSessionDate(prisonCode, sessionDate).toModel()
}

internal object AttendanceUpdateRequestValidator {
  fun validate(request: AttendanceUpdateRequest) {
    // TODO introduce validation of attendance update
  }
}
