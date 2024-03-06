package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
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

  @Transient
  var initialIssuePayment: Boolean? = null,

  var caseNoteId: Long? = null,

  var incentiveLevelWarningIssued: Boolean? = null,

  var otherAbsenceReason: String? = null,
) {
  var issuePayment: Boolean? = null
    set(value) {
      if (!scheduledInstance.isPaid() && value == true) throw IllegalArgumentException("Attendance is not payable, you cannot issue a payment for this attendance")

      field = value
    }

  init {
    issuePayment = initialIssuePayment
  }

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

  fun cancel(reason: AttendanceReason, cancelledReason: String? = null, cancelledBy: String? = null) =
    apply {
      require(hasReason(AttendanceReasonEnum.CANCELLED).not()) { "Attendance already cancelled" }
      require(reason.code == AttendanceReasonEnum.CANCELLED) { "Supplied reason code is not cancelled" }

      mark(
        principalName = cancelledBy ?: ServiceName.SERVICE_NAME.value,
        reason = reason,
        newStatus = AttendanceStatus.COMPLETED,
        newComment = cancelledReason,
        newIssuePayment = isPayable(),
        newIncentiveLevelWarningIssued = null,
        newCaseNoteId = null,
        newOtherAbsenceReason = null,
      )
    }

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

  fun completeWithoutPayment(reason: AttendanceReason) =
    mark(
      principalName = ServiceName.SERVICE_NAME.value,
      reason = reason,
      newStatus = AttendanceStatus.COMPLETED,
      newIssuePayment = false,
      newCaseNoteId = null,
      newComment = null,
      newIncentiveLevelWarningIssued = null,
      newOtherAbsenceReason = null,
    ).also { addAttendanceToHistory() }

  fun unsuspend() =
    apply {
      require(hasReason(AttendanceReasonEnum.SUSPENDED, AttendanceReasonEnum.AUTO_SUSPENDED)) { "Attendance must be suspended to unsuspend it" }
      mark(
        principalName = ServiceName.SERVICE_NAME.value,
        reason = null,
        newStatus = AttendanceStatus.WAITING,
        newIssuePayment = null,
        newCaseNoteId = null,
        newComment = null,
        newIncentiveLevelWarningIssued = null,
        newOtherAbsenceReason = null,
      )
    }

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

    // This check is required because attendances can be created in a suspended or cancelled state as well as waiting.
    if (status != AttendanceStatus.WAITING || history().isNotEmpty()) addAttendanceToHistory()

    if (newStatus == AttendanceStatus.WAITING) {
      resetAttendance(principalName)
      return this
    }

    attendanceReason = reason
    comment = newComment
    issuePayment = newIssuePayment
    incentiveLevelWarningIssued = newIncentiveLevelWarningIssued
    caseNoteId = newCaseNoteId?.toLong()
    otherAbsenceReason = if (AttendanceReasonEnum.OTHER == reason?.code) newOtherAbsenceReason else null
    recordedTime = LocalDateTime.now()
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
        otherAbsenceReason = if (hasReason(AttendanceReasonEnum.OTHER)) otherAbsenceReason else null,
      ),
    )
  }

  fun resetAttendance(resetBy: String? = null) {
    require(editable()) { "Attendance record for prisoner '$prisonerNumber' can no longer be modified" }
    require(status != AttendanceStatus.WAITING) { "Cannot reset an attendance that is already WAITING" }

    attendanceReason = null
    comment = null
    issuePayment = null
    incentiveLevelWarningIssued = null
    bonusAmount = null
    pieces = null
    caseNoteId = null
    otherAbsenceReason = null
    status = AttendanceStatus.WAITING
    recordedTime = LocalDateTime.now()
    recordedBy = resetBy
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

  fun hasReason(vararg reasons: AttendanceReasonEnum) = reasons.any { attendanceReason?.code == it }

  fun isPayable() = scheduledInstance.isPaid()
}

enum class AttendanceStatus {
  WAITING,
  COMPLETED,
}
