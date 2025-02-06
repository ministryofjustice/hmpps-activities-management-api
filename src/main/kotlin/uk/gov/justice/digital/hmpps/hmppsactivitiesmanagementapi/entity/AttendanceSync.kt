package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceSync as AttendanceSyncModel

@Entity
@Immutable
@Table(name = "v_attendance_sync")
data class AttendanceSync(
  @Id
  val attendanceId: Long,

  val scheduledInstanceId: Long,

  val activityScheduleId: Long,

  val sessionDate: LocalDate,

  val sessionStartTime: LocalTime,

  val sessionEndTime: LocalTime,

  val prisonerNumber: String,

  val bookingId: Long,

  val attendanceReasonCode: String?,

  var comment: String?,

  val status: String,

  val payAmount: Int?,

  val bonusAmount: Int?,

  val issuePayment: Boolean?,

  val attendanceReasonDescription: String?,

  val incentiveLevelWarningIssued: Boolean?,

  val caseNoteId: Long? = null,

  val otherAbsenceReason: String?
) {
  fun toModel() = AttendanceSyncModel(
    attendanceId = attendanceId,
    scheduledInstanceId = scheduledInstanceId,
    activityScheduleId = activityScheduleId,
    sessionDate = sessionDate,
    sessionStartTime = sessionStartTime,
    sessionEndTime = sessionEndTime,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    attendanceReasonCode = attendanceReasonCode,
    comment = formatComment(),
    status = status,
    payAmount = payAmount,
    bonusAmount = bonusAmount,
    issuePayment = issuePayment,
  )

  private fun formatComment(): String? {
    if (attendanceReasonCode != null) {
      return when (AttendanceReasonEnum.valueOf(attendanceReasonCode)) {
        AttendanceReasonEnum.CLASH, AttendanceReasonEnum.NOT_REQUIRED -> attendanceReasonDescription
        AttendanceReasonEnum.SICK -> attendanceReasonDescription + " - " + (if (this.issuePayment == true) "Paid" else "Unpaid") + (if (this.comment.isNullOrBlank()) "" else " - " + this.comment)
        AttendanceReasonEnum.OTHER -> "Other - " + (if (this.issuePayment == true) "Paid" else "Unpaid") + (if (this.otherAbsenceReason.isNullOrBlank()) "" else " - " + this.otherAbsenceReason)
        AttendanceReasonEnum.REFUSED -> (if (this.incentiveLevelWarningIssued == true) "Incentive level warning issued - " else "")
        AttendanceReasonEnum.REST, AttendanceReasonEnum.SUSPENDED -> this.attendanceReasonDescription + (if (this.issuePayment == true) " - Paid" else " - Unpaid")
        AttendanceReasonEnum.AUTO_SUSPENDED -> this.attendanceReasonDescription + " from prison" + (if (this.issuePayment == true) " - Paid" else " - Unpaid")
        AttendanceReasonEnum.CANCELLED -> "Activity cancelled - " + (if (this.issuePayment == true) "Paid - " else "Unpaid - ") + this.attendanceReasonDescription + (if (this.comment.isNullOrBlank()) "" else " - " + this.comment)
        else -> null
      }
    }
    return null
  }
}
