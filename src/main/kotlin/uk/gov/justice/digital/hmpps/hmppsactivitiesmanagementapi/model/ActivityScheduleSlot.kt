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
  """,
)
data class ActivityScheduleSlot(

  @Schema(description = "The internally-generated ID for this activity schedule slot", example = "123456")
  val id: Long,

  @Schema(description = "Activity schedule timeslot")
  val timeSlot: TimeSlot,

  @Schema(description = "The week of the schedule this slot relates to", example = "1")
  val weekNumber: Int,

  @Schema(description = "The time that any instances of this schedule slot will start", example = "9:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The time that any instances of this schedule slot will finish", example = "11:30")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The days of the week on which the schedule slot takes place", example = "[Mon,Tue,Wed]")
  val daysOfWeek: List<String>,

  @Schema(description = "Indicates whether the schedule slot takes place on a Monday", example = "true")
  val mondayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Tuesday", example = "true")
  val tuesdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Wednesday", example = "true")
  val wednesdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Thursday", example = "false")
  val thursdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Friday", example = "false")
  val fridayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Saturday", example = "false")
  val saturdayFlag: Boolean,

  @Schema(description = "Indicates whether the schedule slot takes place on a Sunday", example = "false")
  val sundayFlag: Boolean,

  @Schema(description = "a flag to indicate if this activity is scheduled according to prison standard regime times")
  val usePrisonRegimeTime: Boolean = true,
)

enum class TimeSlot {
  @Schema(
    description = "Morning",
  )
  AM,

  @Schema(
    description = "Afternoon",
  )
  PM,

  @Schema(
    description = "Evening",
  )
  ED,
}
