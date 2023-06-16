package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "Prisoner's incentive level suitability")
data class IncentiveLevelSuitability(
  @Schema(description = "The prisoner's suitability", example = "True")
  val suitable: Boolean,
  @Schema(description = "The prisoner's current incentive level", example = "standard")
  val incentiveLevel: String?,
)
