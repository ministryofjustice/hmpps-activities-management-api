package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "An attendance record for a prisoner, can be marked or unmarked")
data class Attendance(

  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long,

  @Schema(description = "The ID for scheduled instance for this attendance", example = "123456")
  val scheduleInstanceId: Long,

  @Schema(description = "The prison number this attendance record is for", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The reason for attending or not")
  val attendanceReason: AttendanceReason? = null,

  @Schema(
    description = "Free text to allow comments to be put against the attendance",
    example = "Prisoner was too unwell to attend the activity.",
  )
  var comment: String? = null,

  @Schema(description = "The date and time the attendance was updated", example = "2023-09-10T09:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime? = null,

  @Schema(description = "The person who updated the attendance", example = "A.JONES")
  val recordedBy: String? = null,

  @Schema(description = "WAITING or COMPLETED", example = "WAITING")
  val status: String,

  @Schema(description = "The amount in pence to pay the prisoner for the activity", example = "100")
  val payAmount: Int? = null,

  @Schema(description = "The bonus amount in pence to pay the prisoner for the activity", example = "50")
  val bonusAmount: Int? = null,

  val pieces: Int? = null,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "Was an incentive level warning issued for REFUSED", example = "true")
  val incentiveLevelWarningIssued: Boolean?,

  @Schema(
    description = "Free text to allow other reasons for non attendance against the attendance",
    example = "Prisoner has a valid reason to miss the activity.",
  )
  val otherAbsenceReason: String? = null,

  @Schema(
    description = "Free text for any case note entered against the attendance record",
    example = "Prisoner has refused to attend the activity without a valid reason to miss the activity.",
  )
  val caseNoteText: String? = null,

  @Schema(description = "The attendance history records for this attendance")
  val attendanceHistory: List<AttendanceHistory> = emptyList(),

  @Schema(description = "Flag to show whether this attendance is editable", example = "true")
  val editable: Boolean = true,

  @Schema(description = "Flag to indicate if the attendance is payable", example = "true")
  val payable: Boolean,
)
