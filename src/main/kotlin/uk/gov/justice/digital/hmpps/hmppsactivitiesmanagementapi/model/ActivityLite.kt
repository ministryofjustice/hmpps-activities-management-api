package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory

@Schema(description = "Describes a top-level activity")
data class ActivityLite(

  @Schema(description = "The internally-generated ID for this activity", example = "123456")
  val id: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "Flag to indicate if attendance is required for this activity, e.g. gym induction might not be mandatory attendance", example = "false")
  val attendanceRequired: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is in cell", example = "false")
  var inCell: Boolean,

  @Schema(description = "Flag to indicate if the activity is piece work", example = "false")
  var pieceWork: Boolean,

  @Schema(description = "Flag to indicate if the activity carried out outside of the prison", example = "false")
  var outsideWork: Boolean,

  @Schema(description = "Indicates whether the activity session is a (F)ull day or a (H)alf day (for payment purposes). ", example = "H")
  var payPerSession: PayPerSession = PayPerSession.H,

  @Schema(description = "A brief summary description of this activity for use in forms and lists", example = "Maths level 1")
  val summary: String,

  @Schema(description = "A detailed description for this activity", example = "A basic maths course suitable for introduction to the subject")
  val description: String?,

  @Schema(description = "The category for this activity, one of the high-level categories")
  val category: ActivityCategory,

  @Schema(description = "The most recent risk assessment level for this activity", example = "High")
  val riskLevel: String?,

  @Schema(description = "The minimum incentive/earned privilege level for this activity", example = "Basic")
  val minimumIncentiveLevel: String?,
)
