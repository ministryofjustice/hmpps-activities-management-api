package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
  description =
  """
  Describes the weekly schedule for an activity. There can be several of these defined for one activity.
  An activity schedule describes when, during the week, an activity will be run and where.
  e.g. Tuesday PM and Thursday AM - suitable for Houseblock 2 to attend.
  e.g. Monday AM and Thursday PM - suitable for Houseblock 3 to attend.
  this 'lite' version of ActivitySchedule does not have allocated or instances.
  """,
)
data class ActivityScheduleLite(

  @Schema(description = "The internally-generated ID for this activity schedule", example = "123456")
  val id: Long,

  @Schema(description = "The description of this activity schedule", example = "Monday AM Houseblock 3")
  val description: String,

  @Schema(description = "The NOMIS internal location for this schedule", example = "98877667")
  var internalLocation: InternalLocation? = null,

  @Schema(description = "The maximum number of prisoners allowed for a scheduled instance of this schedule", example = "10")
  val capacity: Int,

  @Schema(description = "The activity")
  val activity: ActivityLite,

  @Schema(description = "The number of weeks in the schedule", example = "1")
  val scheduleWeeks: Int,

  @Schema(description = "The slots associated with this activity schedule")
  val slots: List<ActivityScheduleSlot> = emptyList(),

  @Schema(description = "The date on which this schedule will start. From this date, any schedules will be created as real, planned instances", example = "2022-09-21")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The date on which this schedule will end. From this date, any schedules will be created as real, planned instances", example = "2022-10-21")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,
)
