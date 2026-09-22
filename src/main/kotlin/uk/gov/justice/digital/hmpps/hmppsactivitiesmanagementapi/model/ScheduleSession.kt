package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.DayOfWeek

@Schema(description = "A single day/time-slot session that was added or removed by an activity schedule amendment")
data class ScheduleSession(
  @Schema(description = "The week of the activity schedule this session relates to", example = "1")
  val weekNumber: Int,

  @Schema(description = "The time slot of the session", example = "AM")
  val timeSlot: TimeSlot,

  @Schema(description = "The day of the week the session runs on", example = "Monday")
  val dayOfWeek: DayOfWeek,
)
