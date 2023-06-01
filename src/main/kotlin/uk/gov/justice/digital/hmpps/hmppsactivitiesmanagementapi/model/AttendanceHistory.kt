package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "An attendance record for a prisoner, can be marked or unmarked")
data class AttendanceHistory(

  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long,

  @Schema(description = "The reason for attending or not")
  val attendanceReason: AttendanceReason? = null,

  @Schema(
    description = "Free text to allow comments to be put against the attendance",
    example = "Prisoner was too unwell to attend the activity.",
  )
  var comment: String? = null,

  @Schema(description = "The date and time the attendance was updated", example = "2023-09-10T09:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime,

  @Schema(description = "The person who updated the attendance", example = "A.JONES")
  val recordedBy: String,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "Was an incentive level warning issued for REFUSED", example = "true")
  val incentiveLevelWarningIssued: Boolean?,

  @Schema(
    description = "Free text to allow other reasons for non attendance against the attendance",
    example = "Prisoner has a valid reason to miss the activity.",
  )
  var otherAbsenceReason: String? = null,

  @Schema(
    description = "Free text for any case note entered against the attendance record",
    example = "Prisoner has refused to attend the activity without a valid reason to miss the activity.",
  )
  val caseNoteText: String? = null,

)
