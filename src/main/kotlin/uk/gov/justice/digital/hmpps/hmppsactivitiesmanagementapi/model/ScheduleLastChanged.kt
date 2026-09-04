package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Details of the most recent activity schedule change that affected this allocation for a specific week of the schedule")
data class ScheduleLastChanged(
  @Schema(description = "The week of the activity schedule this change relates to", example = "1")
  val weekNumber: Int,

  @Schema(description = "When the activity schedule amendment affecting this allocation was made")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val changedAt: LocalDateTime,

  @Schema(description = "Who made the activity schedule amendment", example = "Mrs Blogs")
  val changedBy: String,

  @Schema(description = "The sessions added to the schedule as part of this amendment that the prisoner is now attending")
  val addedSessions: List<ScheduleSession>,

  @Schema(description = "The sessions removed from the schedule as part of this amendment that the prisoner was attending")
  val removedSessions: List<ScheduleSession>,
)
