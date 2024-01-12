package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as AllAttendanceModel

@Entity
@Immutable
@Table(name = "v_all_attendance")
data class AllAttendance(
  @Id
  val attendanceId: Long,

  val prisonCode: String,

  val sessionDate: LocalDate,

  val timeSlot: String,

  val status: String,

  val attendanceReasonCode: String?,

  val issuePayment: Boolean?,

  val prisonerNumber: String,

  val scheduledInstanceId: Long,

  val activityId: Long,

  val summary: String,

  val categoryName: String,

  val recordedTime: LocalDateTime?,
) {
  fun toModel() =
    AllAttendanceModel(
      attendanceId = attendanceId,
      prisonCode = prisonCode,
      sessionDate = sessionDate,
      timeSlot = timeSlot,
      status = status,
      attendanceReasonCode = attendanceReasonCode,
      issuePayment = issuePayment,
      prisonerNumber = prisonerNumber,
      scheduledInstanceId = scheduledInstanceId,
      activityId = activityId,
      activitySummary = summary,
      categoryName = categoryName,
      recordedTime = recordedTime,
    )
}

fun List<AllAttendance>.toModel() = map { it.toModel() }
