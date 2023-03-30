package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
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

  val comment: String?,

  val status: String,

  val payAmount: Int?,

  val bonusAmount: Int?,

  val issuePayment: Boolean?,
) {
  fun toModel() =
      AttendanceSyncModel(
        attendanceId = attendanceId,
        scheduledInstanceId = scheduledInstanceId,
        activityScheduleId = activityScheduleId,
        sessionDate = sessionDate,
        sessionStartTime = sessionStartTime,
        sessionEndTime = sessionEndTime,
        prisonerNumber = prisonerNumber,
        bookingId = bookingId,
        attendanceReasonCode = attendanceReasonCode,
        comment = comment,
        status = status,
        payAmount = payAmount,
        bonusAmount = bonusAmount,
        issuePayment = issuePayment,
      )
}
