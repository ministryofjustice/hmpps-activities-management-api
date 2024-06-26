package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.SuspendedPrisonerActivityAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.SuspendedPrisonerAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
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
  private val activityCategoryRepository: ActivityCategoryRepository,
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
          newIssuePayment = if (updateRequest.issuePayment == true && !attendance.isPayable()) false else updateRequest.issuePayment,
          newIncentiveLevelWarningIssued = updateRequest.incentiveLevelWarningIssued,
          newCaseNoteId = updateRequest.mayBeCaseNote(attendance)?.caseNoteId,
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

  fun getSuspendedPrisonerAttendance(
    prisonCode: String,
    date: LocalDate,
    reason: String? = null,
    categories: List<String>? = null,
  ): List<SuspendedPrisonerAttendance> =
    attendanceRepository.getSuspendedPrisonerAttendance(
      prisonCode = prisonCode,
      date = date,
      reason = reason,
      categories = categories,
    ).groupBy { it.getPrisonerNumber() }.map { attendance ->
      SuspendedPrisonerAttendance(
        prisonerNumber = attendance.key,
        attendance = attendance.value.map {
          SuspendedPrisonerActivityAttendance(
            startTime = it.getStartTime(),
            endTime = it.getEndTime(),
            timeSlot = it.getTimeSlot(),
            categoryName = it.getCategoryName(),
            attendanceReasonCode = it.getAttendanceReasonCode(),
            internalLocation = it.getInternalLocation(),
            inCell = it.getInCell(),
            offWing = it.getOffWing(),
            onWing = it.getOnWing(),
            activitySummary = it.getActivitySummary(),
            scheduledInstanceId = it.getScheduledInstanceId(),
          )
        },
      )
    }

  private fun AttendanceUpdateRequest.mayBeCaseNote(attendance: Attendance): CaseNote? =
    caseNote?.let {
      val caseNoteReason = if (attendance.issuePayment == true && issuePayment == false) "Pay removed" else if (maybeAttendanceReason() == AttendanceReasonEnum.REFUSED) "Refused to attend" else null
      val activityName = attendance.scheduledInstance.activitySummary()
      val location = attendance.scheduledInstance.internalLocationDescription()
      val datetime = attendance.scheduledInstance.dateTime().toMediumFormatStyle()
      val prefix = caseNoteReason?.let {
        listOfNotNull(
          caseNoteReason,
          activityName,
          location,
          datetime,
        ).joinToString(" - ")
      }

      val subType = if (incentiveLevelWarningIssued == true) CaseNoteSubType.IEP_WARN else CaseNoteSubType.NEG_GEN
      caseNotesApiClient.postCaseNote(prisonCode, attendance.prisonerNumber, caseNote, CaseNoteType.NEG, subType, prefix)
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
