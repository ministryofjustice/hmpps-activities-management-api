package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Defines one eligibility rule")
data class EligibilityRule(

  @Schema(description = "The internally-generated ID for this eligibility rule", example = "123456")
  val id: Long,

  @Schema(description = "The code for this eligibility rule", example = "OVER_21")
  val code: String,

  @Schema(description = "The description for this eligibility rule", example = "The prisoner must be over 21 to attend")
  val description: String,
)
