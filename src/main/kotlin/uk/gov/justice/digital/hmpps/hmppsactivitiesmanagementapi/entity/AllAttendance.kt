package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

  val startTime: LocalTime,

  val endTime: LocalTime,

  val status: String,

  val attendanceReasonCode: String?,

  val issuePayment: Boolean?,

  val prisonerNumber: String,

  val scheduledInstanceId: Long,

  val activityId: Long,

  val summary: String,

  val categoryName: String,

  val recordedTime: LocalDateTime?,

  val attendanceRequired: Boolean,

  val eventTier: String?,

  var incentiveLevelWarningIssued: Boolean?,
) {
  fun toModel() =
    AllAttendanceModel(
      attendanceId = attendanceId,
      prisonCode = prisonCode,
      sessionDate = sessionDate,
      timeSlot = timeSlot,
      startTime = startTime,
      endTime = endTime,
      status = status,
      attendanceReasonCode = attendanceReasonCode,
      issuePayment = issuePayment,
      prisonerNumber = prisonerNumber,
      scheduledInstanceId = scheduledInstanceId,
      activityId = activityId,
      activitySummary = summary,
      categoryName = categoryName,
      recordedTime = recordedTime,
      attendanceRequired = attendanceRequired,
      eventTier = eventTier?.let { EventTierType.valueOf(it) },
      incentiveLevelWarningIssued = incentiveLevelWarningIssued,
    )
}

fun List<AllAttendance>.toModel() = map { it.toModel() }
