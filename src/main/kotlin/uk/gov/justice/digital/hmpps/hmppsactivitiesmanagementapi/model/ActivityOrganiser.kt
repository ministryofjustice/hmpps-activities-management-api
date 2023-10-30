package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An activity organiser")
data class ActivityOrganiser(

  @Schema(description = "The internally-generated ID for this activity organiser", example = "1")
  val id: Long,

  @Schema(description = "The code for this activity organiser", example = "PRISON_STAFF")
  val code: String,

  @Schema(description = "The detailed description for this activity organiser", example = "Prison staff")
  val description: String,
)
