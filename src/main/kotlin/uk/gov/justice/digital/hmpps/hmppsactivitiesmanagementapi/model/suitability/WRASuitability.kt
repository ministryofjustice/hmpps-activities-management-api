package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "Prisoner workplace risk assessment suitability")
data class WRASuitability(
  @Schema(description = "The prisoner's suitability", example = "True")
  val suitable: Boolean,
  @Schema(description = "The prisoner's WRA level", example = "medium")
  val riskLevel: String,
)
