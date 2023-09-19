package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
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
  private val telemetryClient: TelemetryClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findAttendancesByScheduledInstance(instanceId: Long) =
    scheduledInstanceRepository.findOrThrowNotFound(instanceId).attendances.map { transform(it, null) }

  fun mark(principalName: String, attendances: List<AttendanceUpdateRequest>) {
    log.info("Attendance marking in progress")

    val markedAttendanceIds = transactionHandler.newSpringTransaction {
      val attendanceUpdatesById = attendances.onEach(AttendanceUpdateRequestValidator::validate).associateBy { it.id }
      val attendanceReasonsByCode = attendanceReasonRepository.findAll().associateBy { it.code }

      attendanceRepository.findAllById(attendanceUpdatesById.keys).onEach { attendance ->
        val updateRequest = attendanceUpdatesById[attendance.attendanceId]!!

        val currentAttendanceStatus = attendance.status()

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

        // If going from WAITING -> COMPLETED track as a RECORD_ATTENDANCE event
        if (currentAttendanceStatus == AttendanceStatus.WAITING && attendance.status() == AttendanceStatus.COMPLETED) {
          val propertiesMap = attendance.toTelemetryPropertiesMap()
          telemetryClient.trackEvent(TelemetryEvent.RECORD_ATTENDANCE.value, propertiesMap)
        }
      }.map { it.attendanceId }
    }

    markedAttendanceIds.forEach { id ->
      outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, id)
    }.also { log.info("Sending attendance amended events.") }

    log.info("Attendance marking done for ${markedAttendanceIds.size} attendance record(s)")
  }

  private fun AttendanceUpdateRequest.mayBeCaseNote(prisonerNumber: String): CaseNote? =
    caseNote?.let {
      caseNotesApiClient.postCaseNote(prisonCode, prisonerNumber, caseNote, incentiveLevelWarningIssued)
    }

  fun getAttendanceById(id: Long) =
    transform(attendanceRepository.findOrThrowNotFound(id), caseNotesApiClient)

  fun getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate): List<ModelAllAttendance> =
    allAttendanceRepository.findByPrisonCodeAndSessionDate(prisonCode, sessionDate).toModel()
}

private fun AttendanceUpdateRequest.maybeAttendanceReason() =
  attendanceReason?.let { AttendanceReasonEnum.valueOf(it.trim().uppercase()) }

internal object AttendanceUpdateRequestValidator {
  fun validate(request: AttendanceUpdateRequest) {
    when (request.status) {
      AttendanceStatus.COMPLETED -> validateCompleted(request)
      AttendanceStatus.WAITING -> validateWaiting(request)
    }
  }

  private fun validateWaiting(request: AttendanceUpdateRequest) {
    validate(
      request.attendanceReason == null &&
        request.comment == null &&
        request.issuePayment == null &&
        request.caseNote == null &&
        request.incentiveLevelWarningIssued == null &&
        request.otherAbsenceReason == null,
    ) {
      "Reason, comment, issue payment, case note, incentive level warning issued and other absence reason must be null if status is waiting for attendance ID ${request.id}"
    }
  }

  private fun validateCompleted(request: AttendanceUpdateRequest) {
    validate(request.attendanceReason != null) {
      "Attendance reason must be supplied when status is completed for attendance ID ${request.id}"
    }

    when (request.maybeAttendanceReason()) {
      AttendanceReasonEnum.ATTENDED -> validateAttended(request)
      AttendanceReasonEnum.NOT_REQUIRED -> validateNotRequired(request)
      AttendanceReasonEnum.REFUSED -> validateRefused(request)
      AttendanceReasonEnum.SICK -> validateSick(request)
      else -> throw ValidationException("Unknown attendance reason")
    }
  }

  private fun validateAttended(request: AttendanceUpdateRequest) {
    validate(
      request.maybeAttendanceReason() == AttendanceReasonEnum.ATTENDED &&
        request.issuePayment != null,
    ) {
      "Issue payment is required when reason is attended for attendance ID ${request.id}"
    }

    validate(
      request.maybeAttendanceReason() == AttendanceReasonEnum.ATTENDED &&
        (request.issuePayment == false && request.caseNote != null) || request.issuePayment == true,
    ) {
      "Case note is required when issue payment is not required when reason is attended for attendance ID ${request.id}"
    }
  }

  private fun validateNotRequired(request: AttendanceUpdateRequest) {
    validate(
      request.maybeAttendanceReason() == AttendanceReasonEnum.NOT_REQUIRED &&
        request.issuePayment == true,
    ) {
      "Issue payment is required when reason is not required for attendance ID ${request.id}"
    }

    validate(
      request.caseNote == null &&
        request.comment == null &&
        request.incentiveLevelWarningIssued == null &&
        request.otherAbsenceReason == null,
    ) {
      "Case note, comments and other absence reason is not required when reason is attended for attendance ID 2"
    }
  }

  private fun validateRefused(request: AttendanceUpdateRequest) {
    validate(
      request.maybeAttendanceReason() == AttendanceReasonEnum.REFUSED &&
        request.comment == null &&
        request.issuePayment == null &&
        request.otherAbsenceReason == null,
    ) {
      "Comment, other absence reason and issue payment must be null if reason is refused for attendance ID ${request.id}"
    }

    validate(
      request.maybeAttendanceReason() == AttendanceReasonEnum.REFUSED &&
        request.caseNote != null &&
        request.incentiveLevelWarningIssued != null,
    ) {
      "Case note and incentive level warning must be supplied if reason is refused for attendance ID ${request.id}"
    }
  }

  private fun validateSick(request: AttendanceUpdateRequest) {
    validate(
      request.maybeAttendanceReason() == AttendanceReasonEnum.SICK &&
        request.caseNote == null &&
        request.otherAbsenceReason == null &&
        request.incentiveLevelWarningIssued == null,
    ) {
      "Case note, incentive level warning issued and other absence reason must be null if reason is sick for attendance ID ${request.id}"
    }

    validate(request.maybeAttendanceReason() == AttendanceReasonEnum.SICK && request.issuePayment != null) {
      "Issue payment must be supplied if reason is sick for attendance ID ${request.id}"
    }
  }

  private fun validate(predicate: Boolean, lazyMessage: () -> Any) {
    if (!predicate) {
      throw ValidationException(lazyMessage().toString())
    }
  }
}
