package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Schema(description = "Describes a top-level activity to be created")
data class ActivityCreateRequest(

  @field:NotEmpty(message = "Prison code must be supplied")
  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String?,

  @Schema(description = "Flag to indicate if attendance is required for this activity, e.g. gym induction might not be mandatory attendance", example = "false")
  val attendanceRequired: Boolean,

  @field:NotEmpty(message = "Activity summary must be supplied")
  @Schema(description = "A brief summary description of this activity for use in forms and lists", example = "Maths level 1")
  val summary: String?,

  @field:NotEmpty(message = "Activity description must be supplied")
  @Schema(description = "A detailed description for this activity", example = "A basic maths course suitable for introduction to the subject")
  val description: String?,

  @field:NotNull(message = "Category ID must be supplied")
  @Schema(description = "The category id for this activity, one of the high-level categories")
  val categoryId: Long?,

  @field:NotNull(message = "Tier ID must be supplied")
  @Schema(description = "The tier id for this activity, as defined by the Future Prison Regime team", example = "1")
  val tierId: Long?,

  @Schema(description = "A list of eligibility rules ids which apply to this activity.", example = "[1, 2, 3]")
  val eligibilityRuleIds: List<Long> = emptyList(),

  @field:Valid
  @Schema(description = "The list of pay rates that can apply to this activity")
  val pay: List<ActivityPayCreateRequest> = emptyList(),

  @Schema(description = "The most recent risk assessment level for this activity", example = "High")
  val riskLevel: String?,

  @Schema(description = "The minimum incentive/earned privilege level for this activity", example = "Basic")
  val minimumIncentiveLevel: String?,

  @Schema(description = "The date on which this activity will start. From this date, any schedules will be created as real, planned instances", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate? = null,

  @Schema(description = "The date on which this activity ends. From this date, there will be no more planned instances of the activity. If null, the activity has no end date and will be scheduled indefinitely.", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,
)
