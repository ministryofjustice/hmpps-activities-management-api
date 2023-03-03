package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "An attendance record for a prisoner, can be marked or unmarked")
data class Attendance(

  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long,

  @Schema(description = "The prison number this attendance record is for", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The reason for attending or not")
  val attendanceReason: AttendanceReason? = null,

  @Schema(
    description = "Free text to allow comments to be put against the attendance",
    example = "Prisoner was too unwell to attend the activity.",
  )
  var comment: String? = null,

  val posted: Boolean,

  @Schema(description = "The date and time the attendance was updated", example = "2023-09-10T09:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime? = null,

  @Schema(description = "The person who updated the attendance", example = "A.JONES")
  val recordedBy: String? = null,

  @Schema(description = "SCHEDULED, COMPLETED, CANCELLED.", example = "SCHEDULED")
  val status: String,

  @Schema(description = "The amount in pence to pay the prisoner for the activity", example = "100")
  val payAmount: Int? = null,

  @Schema(description = "The bonus amount in pence to pay the prisoner for the activity", example = "50")
  val bonusAmount: Int? = null,

  val pieces: Int? = null,
)
