package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import java.time.DayOfWeek
import java.time.LocalDate

@Schema(description = "Describes a top-level activity to be created")
data class ActivityCreateRequest(

  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String?,

  @Schema(
    description = "Flag to indicate if attendance is required for this activity, e.g. gym induction might not be mandatory attendance",
    example = "false",
  )
  val attendanceRequired: Boolean = true,

  @Schema(description = "Flag to indicate if the location of the activity is in cell", example = "false")
  var inCell: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  var onWing: Boolean,

  @Schema(description = "Flag to indicate if the activity is piece work", example = "false")
  var pieceWork: Boolean,

  @Schema(description = "Flag to indicate if the activity carried out outside of the prison", example = "false")
  var outsideWork: Boolean,

  @Schema(
    description = "Indicates whether the activity session is a (F)ull day or a (H)alf day (for payment purposes). ",
    example = "H",
  )
  var payPerSession: PayPerSession?,

  @field:NotEmpty(message = "Activity summary must be supplied")
  @field:Size(max = 50, message = "Summary should not exceed {max} characters")
  @Schema(
    description = "A brief summary description of this activity for use in forms and lists",
    example = "Maths level 1",
  )
  val summary: String?,

  @field:Size(max = 300, message = "Description should not exceed {max} characters")
  @Schema(
    description = "A detailed description for this activity",
    example = "A basic maths course suitable for introduction to the subject",
  )
  val description: String?,

  @field:NotNull(message = "Category ID must be supplied")
  @Schema(description = "The category id for this activity, one of the high-level categories")
  val categoryId: Long?,

  @Schema(description = "The tier id for this activity, as defined by the Future Prison Regime team", example = "1")
  val tierId: Long?,

  @Schema(description = "A list of eligibility rules ids which apply to this activity.", example = "[1, 2, 3]")
  val eligibilityRuleIds: List<Long> = emptyList(),

  @field:Valid
  @Schema(description = "The list of pay rates that can apply to this activity")
  val pay: List<ActivityPayCreateRequest> = emptyList(),

  @field:NotEmpty(message = "Risk level must be supplied")
  @field:Size(max = 10, message = "Risk level should not exceed {max} characters")
  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String?,

  @field:NotEmpty(message = "Minimum incentive level NOMIS code must be supplied")
  @field:Size(max = 3, message = "Minimum incentive level NOMIS code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS code for the minimum incentive/earned privilege level for this activity",
    example = "BAS",
  )
  val minimumIncentiveNomisCode: String?,

  @field:NotEmpty(message = "Minimum incentive level must be supplied")
  @field:Size(max = 10, message = "Minimum incentive level should not exceed {max} characters")
  @Schema(description = "The minimum incentive/earned privilege level for this activity", example = "Basic")
  val minimumIncentiveLevel: String?,

  @field:NotNull
  @field:Future(message = "Activity start date must be in the future")
  @Schema(
    description = "The future date on which this activity will start. From this date, any schedules will be created as real, planned instances",
    example = "2022-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate?,

  @Schema(
    description = "The date on which this activity ends. From this date, there will be no more planned instances of the activity. If null, the activity has no end date and will be scheduled indefinitely.",
    example = "2022-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @field:Valid
  @Schema(description = "The list of minimum education levels that apply to this activity")
  val minimumEducationLevel: List<ActivityMinimumEducationLevelCreateRequest> = emptyList(),

  @Schema(description = "The optional NOMIS internal location id for this schedule", example = "98877667")
  val locationId: Long?,

  @field:Positive(message = "The capacity must be a positive integer")
  @Schema(
    description = "The maximum number of prisoners allowed for a scheduled instance of this schedule",
    example = "10",
  )
  val capacity: Int?,

  @field:Min(value = 1, message = "Schedule weeks must be either 1 or 2")
  @field:Max(value = 2, message = "Schedule weeks must be either 1 or 2")
  @Schema(description = "The number of weeks in the schedule", example = "1")
  val scheduleWeeks: Int,

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
  @field:Positive(message = "The week number must be a positive integer")
  @Schema(description = "The week of the schedule this slot relates to", example = "1")
  val weekNumber: Int,

  @field:NotEmpty(message = "The time slot must supplied")
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
) {
  fun getDaysOfWeek(): Set<DayOfWeek> {
    return setOfNotNull(
      DayOfWeek.MONDAY.takeIf { monday },
      DayOfWeek.TUESDAY.takeIf { tuesday },
      DayOfWeek.WEDNESDAY.takeIf { wednesday },
      DayOfWeek.THURSDAY.takeIf { thursday },
      DayOfWeek.FRIDAY.takeIf { friday },
      DayOfWeek.SATURDAY.takeIf { saturday },
      DayOfWeek.SUNDAY.takeIf { sunday },
    )
  }

  fun timeSlot() = TimeSlot.valueOf(timeSlot!!)
}
