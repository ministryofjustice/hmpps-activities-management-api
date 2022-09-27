package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

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

  @Schema(description = "If not null, it indicates the date until which this schedule is suspended", example = "10/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspendUntil: LocalDate? = null,

  @Schema(description = "The time that any instances of this schedule will start", example = "9:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The time that any instances of this schedule will finish", example = "11:30")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The NOMIS internal location id for this schedule", example = "98877667")
  val internalLocationId: Int? = null,

  @Schema(description = "The NOMIS internal location code for this schedule", example = "EDU-ROOM-1")
  val internalLocationCode: String? = null,

  @Schema(description = "The NOMIS internal location description for this schedule", example = "Education - R1")
  val internalLocationDescription: String? = null,

  @Schema(description = "The maximum number of prisoners allowed for a scheduled instance of this schedule", example = "10")
  val capacity: Int,

  @Schema(description = "The days of the week on which the schedule takes place", example = "Mon,Tue,Wed")
  val daysOfWeek: String
)
