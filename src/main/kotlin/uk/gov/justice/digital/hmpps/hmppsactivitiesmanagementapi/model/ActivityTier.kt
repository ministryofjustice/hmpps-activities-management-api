package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class ActivityTier(

  @Schema(description = "The internal ID for this activity tier", example = "123456")
  val id: Long,

  @Schema(description = "The code for this activity tier", example = "Tier 1")
  val code: String,

  @Schema(description = "The detailed description for this activity tier", example = "Work, education, intervention")
  val description: String
)
