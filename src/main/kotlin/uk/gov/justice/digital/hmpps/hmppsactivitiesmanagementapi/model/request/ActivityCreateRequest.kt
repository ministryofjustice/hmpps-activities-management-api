package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
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

  @Schema(description = "Flag to indicate if the location of the activity is off wing and not in a listed location", example = "false")
  var offWing: Boolean,

  @Schema(description = "Flag to indicate if the activity is piece work", example = "false")
  var pieceWork: Boolean,

  @Schema(description = "Flag to indicate if the activity carried out outside of the prison", example = "false")
  var outsideWork: Boolean,

  @Schema(description = "Flag to indicate if the activity is a paid activity. It true then pay rates are required, if false then no pay rates should be provided", example = "true")
  val paid: Boolean = true,

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

  @field:NotEmpty(message = "Tier code must be supplied")
  @Schema(description = "The tier code for this activity, as defined by the Future Prison Regime team", example = "TIER_1")
  val tierCode: String?,

  @Schema(description = "The organiser code for the organiser of this activity", example = "PRISON_STAFF")
  val organiserCode: String?,

  @Schema(description = "A list of eligibility rules ids which apply to this activity.", example = "[1, 2, 3]")
  val eligibilityRuleIds: List<Long> = emptyList(),

  @field:Valid
  @Schema(description = "The list of pay rates that can apply to this activity")
  val pay: List<ActivityPayCreateRequest> = emptyList(),

  @field:NotEmpty(message = "Risk level must be supplied")
  @field:Size(max = 10, message = "Risk level should not exceed {max} characters")
  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String?,

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
) {
  @AssertTrue(message = "Unpaid activity cannot have pay rates associated with it")
  private fun isUnpaid() = pay.isEmpty() || paid

  @AssertTrue(message = "Paid activity must have at least one pay rate associated with it")
  private fun isPaid() = pay.isNotEmpty() || !paid

  @AssertTrue(message = "Activity with tierCode TIER_1 or TIER_2 must be attended")
  private fun isAttendCheck() = tierCode == null ||
    EventTierType.valueOf(tierCode) == EventTierType.FOUNDATION ||
    (
      (
        EventTierType.valueOf(tierCode) == EventTierType.TIER_1 || EventTierType.valueOf(tierCode) == EventTierType.TIER_2
        ) && attendanceRequired
      )
}
