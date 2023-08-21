package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import java.time.LocalDateTime

@Schema(description = "Summarises an activity")
data class ActivitySummary(

  @Schema(description = "The internally-generated ID for this activity", example = "123456")
  val id: Long,

  @Schema(description = "The name of the activity", example = "Maths level 1")
  val activityName: String?,

  @Schema(description = "The category for this activity, one of the high-level categories")
  val category: ActivityCategory,

  @Schema(description = "The capacity of the activity")
  val capacity: Int,

  @Schema(description = "The number of prisoners currently allocated to the activity")
  val allocated: Int,

  @Schema(description = "The number of prisoners currently currently on the waitlist for the activity")
  val waitlisted: Int,

  @Schema(description = "The date and time when this activity was created", example = "2022-09-01T09:01:02")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

  @Schema(description = "Whether the activity is live or archived", example = "LIVE")
  val activityState: ActivityState,
)
