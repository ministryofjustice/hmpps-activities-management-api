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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import java.time.LocalDate
import java.time.LocalDateTime

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
  @OneToMany(mappedBy = "attendance", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
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
    reason = if (attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) reason else attendanceReason,
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
    require(editable()) { "Attendance record for prisoner '$prisonerNumber' can no longer be modified" }

    if (status != AttendanceStatus.WAITING || history().isNotEmpty()) addAttendanceToHistory()

    if (newStatus == AttendanceStatus.WAITING) {
      resetAttendance()
    } else {
      attendanceReason = reason
      comment = newComment
      issuePayment = newIssuePayment
      incentiveLevelWarningIssued = newIncentiveLevelWarningIssued
      caseNoteId = newCaseNoteId?.toLong()
      otherAbsenceReason = if (AttendanceReasonEnum.OTHER == reason?.code) newOtherAbsenceReason else null
      recordedTime = LocalDateTime.now()
    }
    status = newStatus
    recordedBy = principalName
    return this
  }

  private fun addAttendanceToHistory() {
    this.addHistory(
      AttendanceHistory(
        attendance = this,
        attendanceReason = attendanceReason,
        comment = comment,
        recordedTime = recordedTime ?: LocalDateTime.now(),
        recordedBy = recordedBy ?: "",
        issuePayment = issuePayment,
        caseNoteId = caseNoteId,
        incentiveLevelWarningIssued = incentiveLevelWarningIssued,
        otherAbsenceReason = if (AttendanceReasonEnum.OTHER == attendanceReason?.code) otherAbsenceReason else null,
      ),
    )
  }

  fun resetAttendance() {
    if (attendanceReason?.code != AttendanceReasonEnum.SUSPENDED) attendanceReason = null
    comment = null
    issuePayment = null
    incentiveLevelWarningIssued = null
    bonusAmount = null
    pieces = null
    caseNoteId = null
    otherAbsenceReason = null
    recordedTime = LocalDateTime.now()
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

  fun completeWithoutPayment(reason: AttendanceReason) =
    apply {
      require(editable()) { "Attendance record for prisoner '$prisonerNumber' can no longer be modified" }

      attendanceReason = reason
      issuePayment = false
      status = AttendanceStatus.COMPLETED
      recordedTime = LocalDateTime.now()
      recordedBy = ServiceName.SERVICE_NAME.value
      addAttendanceToHistory()
    }
}

enum class AttendanceStatus {
  WAITING,
  COMPLETED,
}
