package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "An advance attendance history record for a prisoner")
data class AdvanceAttendanceHistory(

  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean,

  @Schema(description = "The date and time the attendance was updated", example = "2023-09-10T09:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime,

  @Schema(description = "The person who updated the attendance", example = "A.JONES")
  val recordedBy: String,
)
