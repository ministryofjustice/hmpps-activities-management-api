package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
  description =
  """
  Describes a single schedule for an activity. There can be several of these defined for one activity.
  
  The schedules description must be unique in the context of any other schedules that may already exist on the
  activity.
  
  A schedule has a mandatory start date and optional end date.  These dates cannot go outside the boundaries
  of the activity it is being added to.

  An activity schedule has one or more slot which describes when, during the week, an activity will be run and where:
  
  e.g. Monday AM and Thursday PM, Tuesday PM and Thursday AM.
  """,
)
data class ActivityScheduleCreateRequest(

  @field:NotEmpty(message = "Activity description must be supplied")
  @field:Size(max = 50, message = "Description should not exceed {max} characters")
  @Schema(description = "The unique description of this activity schedule", example = "Entry level Maths 1")
  val description: String?,

  @field:NotNull(message = "The start date must be supplied")
  @Schema(
    description = "The date on which this activity scheduled will start. This cannot be before to the activity start date.",
    example = "2022-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate? = null,

  @Schema(
    description = "The (optional) date on which this activity scheduled will end. If supplied this must be after to the start date.",
    example = "2023-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "The optional NOMIS internal location id for this schedule", example = "98877667")
  val locationId: Long?,

  @field:Positive(message = "The capacity must be a positive integer")
  @Schema(
    description = "The maximum number of prisoners allowed for a scheduled instance of this schedule",
    example = "10",
  )
  val capacity: Int?,

  @field:NotEmpty(message = "The activity schedule must have one or more unique slots")
  @Schema(description = "The days and times an activity schedule can take place")
  val slots: List<Slot>?,

  @Schema(description = "Whether the schedule runs on bank holidays", example = "true")
  val runsOnBankHoliday: Boolean = false,
)

@Schema(
  description = """
    Describes time slot and day (or days) the scheduled activity would run. At least one day must be specified.
    
    e.g. 'AM, Monday, Wednesday and Friday' or 'PM Tuesday, Thursday, Sunday'
  """,
)

data class Slot(

  @field:NotNull(message = "The time slot must supplied")
  @Schema(
    description = "The time slot of the activity schedule, morning afternoon or evening e.g. AM, PM or ED",
    example = "AM",
  )
  val timeSlot: String?,

  val monday: Boolean = false,

  val tuesday: Boolean = false,

  val wednesday: Boolean = false,

  val thursday: Boolean = false,

  val friday: Boolean = false,

  val saturday: Boolean = false,

  val sunday: Boolean = false,
)
