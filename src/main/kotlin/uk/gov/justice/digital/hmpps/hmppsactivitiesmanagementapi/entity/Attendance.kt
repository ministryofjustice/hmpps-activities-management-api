package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "attendance")
data class Attendance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceId: Long = -1,

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

  fun waiting() {
    attendanceReason = null
    status = AttendanceStatus.WAITING
    comment = null
    recordedBy = null
    recordedTime = null
    payAmount = null
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(attendanceId = $attendanceId )"
  }

  fun cancel(reason: AttendanceReason, payIncentiveCode: String?) {
    status = AttendanceStatus.COMPLETED
    payAmount = if (payIncentiveCode != null) getPay(payIncentiveCode)?.rate ?: 0 else 0
    attendanceReason = reason
    comment = scheduledInstance.cancelledReason
    recordedTime = scheduledInstance.cancelledTime
    recordedBy = scheduledInstance.cancelledBy
  }

  fun mark(
    reason: AttendanceReason?,
    newStatus: AttendanceStatus,
    newComment: String?,
    newIssuePayment: Boolean?,
    newIncentiveLevelWarningIssued: Boolean?,
  ): Attendance {
    attendanceReason = reason
    status = newStatus
    comment = newComment
    issuePayment = newIssuePayment
    incentiveLevelWarningIssued = newIncentiveLevelWarningIssued
    return this
  }

  private fun getPay(incentiveCode: String): ActivityPay? {
    val currentAllocation = scheduledInstance.activitySchedule.allocations()
      .filter { it.isAllocated() }
      .find { it.prisonerNumber == prisonerNumber }

    return scheduledInstance.activitySchedule.activity.activityPay()
      .filter { it.payBand.prisonPayBandId == currentAllocation?.payBand?.prisonPayBandId }
      .find { it.incentiveNomisCode == incentiveCode }
  }
}

enum class AttendanceStatus {
  WAITING,
  COMPLETED,
  LOCKED,
}
