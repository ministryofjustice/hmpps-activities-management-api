package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Describes a top-level activity to be updated")
data class ActivityUpdateRequest(

  @Schema(description = "The category id for this activity, one of the high-level categories")
  val categoryId: Long? = null,

  @Schema(description = "The tier id for this activity, as defined by the Future Prison Regime team", example = "1")
  val tierId: Long? = null,

  @field:Size(max = 50, message = "Summary should not exceed {max} characters")
  @Schema(
    description = "A brief summary description of this activity for use in forms and lists",
    example = "Maths level 1",
  )
  val summary: String? = null,

  @Schema(
    description = "The date on which this activity will start. From this date, any schedules will be created as real, planned instances",
    example = "2022-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @field:Future(message = "Activity start date must be in the future")
  val startDate: LocalDate? = null,

  @Schema(
    description = "The date on which this activity ends. From this date, there will be no more planned instances of the activity. If null, the activity has no end date and will be scheduled indefinitely.",
    example = "2022-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @field:Size(max = 3, message = "Minimum incentive level NOMIS code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS code for the minimum incentive/earned privilege level for this activity",
    example = "BAS",
  )
  val minimumIncentiveNomisCode: String? = null,

  @field:Size(max = 10, message = "Minimum incentive level should not exceed {max} characters")
  @Schema(description = "The minimum incentive/earned privilege level for this activity", example = "Basic")
  val minimumIncentiveLevel: String? = null,

  @Schema(description = "Whether the schedule runs on bank holidays", example = "true")
  val runsOnBankHoliday: Boolean? = null,

  @field:Positive(message = "The capacity must be a positive integer")
  @Schema(
    description = "The maximum number of prisoners allowed for a scheduled instance of this schedule",
    example = "10",
  )
  val capacity: Int? = null,

  @field:Size(max = 10, message = "Risk level should not exceed {max} characters")
  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String? = null,

  @Schema(description = "The optional NOMIS internal location id for this schedule", example = "98877667")
  val locationId: Long? = null,

  @Schema(description = "Flag to indicate if the location of the activity is in cell", example = "false")
  var inCell: Boolean? = null,

  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  var onWing: Boolean? = null,

  @Schema(description = "Flag to indicate if the location of the activity is off wing and not in a listed location", example = "false")
  var offWing: Boolean? = null,

  @Schema(
    description = "Flag to indicate if attendance is required for this activity, e.g. gym induction might not be mandatory attendance",
    example = "false",
  )
  val attendanceRequired: Boolean? = null,

  @field:Valid
  @Schema(description = "The list of minimum education levels that apply to this activity")
  val minimumEducationLevel: List<ActivityMinimumEducationLevelCreateRequest>? = null,

  @field:Valid
  @Schema(description = "The list of pay rates that can apply to this activity")
  val pay: List<ActivityPayCreateRequest>? = null,

  @field:Min(value = 1, message = "Schedule weeks must be either 1 or 2")
  @field:Max(value = 2, message = "Schedule weeks must be either 1 or 2")
  @Schema(description = "The number of weeks in the schedule", example = "1")
  val scheduleWeeks: Int? = null,

  @Schema(description = "The days and times an activity schedule can take place")
  val slots: List<Slot>? = null,

  @Schema(description = "A flag to indicate that the end date is to be removed", example = "true", defaultValue = "false")
  val removeEndDate: Boolean = false,
)
