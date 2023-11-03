package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An event tier")
data class EventTier(

  @Schema(description = "The internally-generated ID for this event tier", example = "1")
  val id: Long,

  @Schema(description = "The code for this event tier", example = "TIER_1")
  val code: String,

  @Schema(description = "The detailed description for this event tier", example = "Work, education and maintenance")
  val description: String,
)
