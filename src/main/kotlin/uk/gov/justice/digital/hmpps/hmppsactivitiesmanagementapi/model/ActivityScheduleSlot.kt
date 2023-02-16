package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(
  description =
  """
  Describes a slot for an activity schedule. There can be several of these defined for one activity schedule.
  An activity schedule slot describes when, during the week, an activity will be run.
  e.g. Tuesday PM on a Monday and Thursday.
  """
)
data class ActivityScheduleSlot(

  @Schema(description = "The internally-generated ID for this activity schedule slot", example = "123456")
  val id: Long,

  @Schema(description = "The time that any instances of this schedule slot will start", example = "9:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The time that any instances of this schedule slot will finish", example = "11:30")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The days of the week on which the schedule slot takes place", example = "[Mon,Tue,Wed]")
  val daysOfWeek: List<String>,

  @Schema(description = "Indicates whether the schedule slot takes place on a Monday")
  val mondayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Tuesday")
  val tuesdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Wednesday")
  val wednesdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Thursday")
  val thursdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Friday")
  val fridayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Saturday")
  val saturdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Sunday")
  val sundayFlag: Boolean,
)
