package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as AllAttendanceModel

@Entity
@Immutable
@Table(name = "v_all_attendance")
data class `AllAttendance`(
  @Id
  val attendanceId: Long,

  val sessionDate: LocalDate,

  val timeSlot: String,

  val status: String,

  val attendanceReasonCode: String?,

  val issuePayment: Boolean?,
) {
  fun toModel() =
    AllAttendanceModel(
      attendanceId = attendanceId,
      sessionDate = sessionDate,
      timeSlot = timeSlot,
      status = status,
      attendanceReasonCode = attendanceReasonCode,
      issuePayment = issuePayment,
    )
}
