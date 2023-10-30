package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An activity tier")
data class ActivityTier(

  @Schema(description = "The internally-generated ID for this activity tier", example = "1")
  val id: Long,

  @Schema(description = "The code for this activity tier", example = "TIER_1")
  val code: String,

  @Schema(description = "The detailed description for this activity tier", example = "Work, education and maintenance")
  val description: String,
)
