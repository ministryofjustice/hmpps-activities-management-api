package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
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
  private var status: AttendanceStatus = AttendanceStatus.WAITING,

  var payAmount: Int? = null,

  var bonusAmount: Int? = null,

  var pieces: Int? = null,

  var issuePayment: Boolean? = null,

  var caseNoteId: Long? = null,

  var incentiveLevelWarningIssued: Boolean? = null,

  var otherAbsenceReason: String? = null,
) {
  @OneToMany(mappedBy = "attendance", cascade = [CascadeType.ALL], orphanRemoval = true)
  @BatchSize(size = 5)
  private var attendanceHistory: MutableList<AttendanceHistory> = mutableListOf()

  fun status() = status
  fun status(vararg status: AttendanceStatus) = status.any { it == this.status }

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
    reason = if (this.attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) reason else this.attendanceReason,
    newStatus = AttendanceStatus.COMPLETED,
    newComment = scheduledInstance.cancelledReason,
    newIssuePayment = this.attendanceReason?.code != AttendanceReasonEnum.SUSPENDED,
    newIncentiveLevelWarningIssued = null,
    newCaseNoteId = null,
    newOtherAbsenceReason = null,
  )

  fun uncancel() = mark(
    principalName = if (this.attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) null else this.recordedBy,
    reason = if (this.attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) null else this.attendanceReason,
    newStatus = if (this.attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) AttendanceStatus.WAITING else this.status,
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
          otherAbsenceReason = if (AttendanceReasonEnum.OTHER == attendanceReason?.code) otherAbsenceReason else null,
        ),
      )
    }

    if (newStatus == AttendanceStatus.WAITING) {
      attendanceReason = if (this.attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) null else attendanceReason
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
      otherAbsenceReason = if (AttendanceReasonEnum.OTHER == reason?.code) newOtherAbsenceReason else null
    }
    status = newStatus
    recordedBy = principalName
    recordedTime = if (newStatus != AttendanceStatus.WAITING) LocalDateTime.now() else null
    return this
  }

  /*
   Very simple rules for editable attendance initially. Attendance is editable if :
   1. It has not been marked, and we are within 14 days of the session date.
   2. It has been marked (paid or unpaid) and today is the date of the session.
   3. It has been marked with an unpaid reason, and we are within 14 days of the session date.
   4. It has been marked with a paid reason, it was marked today, and we are within 14 days of the session date.
   */
  fun editable(): Boolean {
    return (
      this.status() == AttendanceStatus.WAITING &&
        this.scheduledInstance.sessionDate.isAfter(LocalDate.now().minusDays(14)) ||
        this.status == AttendanceStatus.COMPLETED &&
        this.scheduledInstance.sessionDate.isEqual(LocalDate.now()) ||
        this.status == AttendanceStatus.COMPLETED &&
        this.issuePayment == false &&
        this.scheduledInstance.sessionDate.isAfter(LocalDate.now().minusDays(14)) ||
        this.status == AttendanceStatus.COMPLETED &&
        this.issuePayment == true &&
        this.recordedTime!!.isAfter(LocalDate.now().atStartOfDay()) &&
        this.scheduledInstance.sessionDate.isAfter(LocalDate.now().minusDays(14))
      )
  }

  fun toModel(caseNoteText: String?) = ModelAttendance(
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
    caseNoteText = caseNoteText,
    attendanceHistory = this.attendanceHistory
      .sortedWith(compareBy { it.recordedTime })
      .reversed()
      .map { attendanceHistoryRow -> transform(attendanceHistoryRow, caseNoteText) },
    editable = this.editable(),
  )
}

enum class AttendanceStatus {
  WAITING,
  COMPLETED,
}
