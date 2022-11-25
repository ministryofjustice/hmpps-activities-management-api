package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a top-level activity category")
data class ActivityCategory(
  @Schema(
    description = "The internally-generated identifier for this activity category",
    example = "1"
  )
  val id: Long,

  @Schema(description = "The activity category code", example = "LEI")
  val code: String,

  @Schema(description = "The name of the activity category", example = "Leisure and social")
  val description: String
)
