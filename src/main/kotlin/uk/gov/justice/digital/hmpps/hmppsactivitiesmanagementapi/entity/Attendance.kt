package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as ModelAttendanceReason

@Entity
@Table(name = "attendance")
@EntityListeners(AttendanceEntityListener::class)
data class Attendance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "scheduled_instance_id", nullable = false)
  val scheduledInstance: ScheduledInstance,

  val prisonerNumber: String,

  @OneToOne
  @JoinColumn(name = "attendance_reason_id", nullable = true)
  var attendanceReason: AttendanceReason? = null,

  var comment: String? = null,

  var recordedTime: LocalDateTime? = null,

  var recordedBy: String? = null,

  @Enumerated(EnumType.STRING)
  var status: AttendanceStatus = AttendanceStatus.WAITING,

  var payAmount: Int? = null,

  var bonusAmount: Int? = null,

  var pieces: Int? = null,

  var issuePayment: Boolean? = null,

  var caseNoteId: Long? = null,

  var incentiveLevelWarningIssued: Boolean? = null,

  var otherAbsenceReason: String? = null,

) {
  @OneToMany(mappedBy = "attendance", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private var attendanceHistory: MutableList<AttendanceHistory> = mutableListOf()

  fun history() = attendanceHistory.toList()

  fun addHistory(history: AttendanceHistory) {
    attendanceHistory.add(history)
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(attendanceId = $attendanceId )"
  }

  fun cancel(reason: AttendanceReason) = mark(
    principalName = scheduledInstance.cancelledBy,
    reason = reason,
    newStatus = AttendanceStatus.COMPLETED,
    newComment = scheduledInstance.cancelledReason,
    newIssuePayment = true,
    newIncentiveLevelWarningIssued = null,
    newCaseNoteId = null,
    newOtherAbsenceReason = null,
  )

  fun uncancel() = mark(
    principalName = null,
    reason = null,
    newStatus = AttendanceStatus.WAITING,
    newComment = null,
    newIssuePayment = null,
    newIncentiveLevelWarningIssued = null,
    newCaseNoteId = null,
    newOtherAbsenceReason = null,
  )

  fun mark(
    principalName: String?,
    reason: AttendanceReason?,
    newStatus: AttendanceStatus,
    newComment: String?,
    newIssuePayment: Boolean?,
    newIncentiveLevelWarningIssued: Boolean?,
    newCaseNoteId: String?,
    newOtherAbsenceReason: String?,
  ): Attendance {
    if (status != AttendanceStatus.WAITING) {
      this.addHistory(
        AttendanceHistory(
          attendance = this,
          attendanceReason = attendanceReason,
          comment = comment,
          recordedTime = recordedTime!!,
          recordedBy = recordedBy!!,
          issuePayment = issuePayment,
          caseNoteId = caseNoteId,
          incentiveLevelWarningIssued = incentiveLevelWarningIssued,
          otherAbsenceReason = otherAbsenceReason,
        ),
      )
    }
    if (newStatus == AttendanceStatus.WAITING) {
      attendanceReason = null
      comment = null
      issuePayment = null
      incentiveLevelWarningIssued = null
      bonusAmount = null
      pieces = null
      caseNoteId = null
      otherAbsenceReason = null
    } else {
      attendanceReason = reason
      comment = newComment
      issuePayment = newIssuePayment
      incentiveLevelWarningIssued = newIncentiveLevelWarningIssued
      caseNoteId = newCaseNoteId?.toLong()
      otherAbsenceReason = newOtherAbsenceReason
    }
    status = newStatus
    recordedBy = principalName
    recordedTime = if (newStatus != AttendanceStatus.WAITING) LocalDateTime.now() else null
    return this
  }

  fun toModel(caseNotesApiClient: CaseNotesApiClient) = ModelAttendance(
    id = this.attendanceId,
    scheduleInstanceId = this.scheduledInstance.scheduledInstanceId,
    prisonerNumber = this.prisonerNumber,
    attendanceReason = this.attendanceReason?.let {
      ModelAttendanceReason(
        id = it.attendanceReasonId,
        code = it.code.toString(),
        description = it.description,
        attended = it.attended,
        capturePay = it.capturePay,
        captureMoreDetail = it.captureMoreDetail,
        captureCaseNote = it.captureCaseNote,
        captureIncentiveLevelWarning = it.captureIncentiveLevelWarning,
        captureOtherText = it.captureOtherText,
        displayInAbsence = it.displayInAbsence,
        displaySequence = it.displaySequence,
        notes = it.notes,
      )
    },
    comment = this.comment,
    recordedTime = this.recordedTime,
    recordedBy = this.recordedBy,
    status = this.status.name,
    payAmount = this.payAmount,
    bonusAmount = this.bonusAmount,
    pieces = this.pieces,
    issuePayment = this.issuePayment,
    incentiveLevelWarningIssued = this.incentiveLevelWarningIssued,
    otherAbsenceReason = this.otherAbsenceReason,
    caseNoteText = this.caseNoteId ?.let { caseNotesApiClient.getCaseNote(this.prisonerNumber, this.caseNoteId)?.text },
    attendanceHistory = this.attendanceHistory
      .sortedWith(compareBy { it.recordedTime })
      .reversed()
      .map { attendanceHistoryRow -> transform(attendanceHistoryRow) },
  )
}

enum class AttendanceStatus {
  WAITING,
  COMPLETED,
  LOCKED,
}
