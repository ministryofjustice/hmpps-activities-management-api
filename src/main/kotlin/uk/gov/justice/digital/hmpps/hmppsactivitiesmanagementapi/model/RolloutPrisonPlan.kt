package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Describes the rollout plan of a prison which may or may not be rolled out")
data class RolloutPrisonPlan(

  @Schema(description = "The prison code of the requested prison", example = "PVI")
  var prisonCode: String,

  @Schema(description = "Flag to indicate if activities are enabled", example = "true")
  var activitiesRolledOut: Boolean,

  @Schema(description = "Flag to indicate if appointments are enabled", example = "true")
  var appointmentsRolledOut: Boolean,

  @Schema(description = "max days to expire events based on prisoner movement, default is 21")
  val maxDaysToExpiry: Int = 21,

  @Schema(description = "Flag to indicate if this prison is presently rolled out and live to the prison", example = "true")
  var prisonLive: Boolean,
)
