package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Describes the weekly schedule for an activity. There can be several of these defined for one activity.
  An activity schedule describes when, during the week, an activity will be run and where.
  e.g. Tuesday PM and Thursday AM - suitable for Houseblock 2 to attend.
  e.g. Monday AM and Thursday PM - suitable for Houseblock 3 to attend.
  """
)
data class ActivitySchedule(

  @Schema(description = "The internally-generated ID for this activity schedule", example = "123456")
  val id: Long,

  @Schema(description = "The planned instances associated with this activity schedule")
  val instances: List<ScheduledInstance> = emptyList(),

  @Schema(description = "The list of allocated prisoners who are allocated to this schedule, at this time and location")
  val allocations: List<Allocation> = emptyList(),

  @Schema(description = "The description of this activity schedule", example = "Monday AM Houseblock 3")
  val description: String,

  @Schema(description = "Indicates the dates between which the schedule has been suspended")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspensions: List<Suspension> = emptyList(),

  @Schema(description = "The NOMIS internal location for this schedule", example = "98877667")
  val internalLocation: InternalLocation? = null,

  @Schema(description = "The maximum number of prisoners allowed for a scheduled instance of this schedule", example = "10")
  val capacity: Int,

  @Schema(description = "The activity")
  val activity: ActivityLite,

  @Schema(description = "The slots associated with this activity schedule")
  val slots: List<ActivityScheduleSlot> = emptyList(),
)
