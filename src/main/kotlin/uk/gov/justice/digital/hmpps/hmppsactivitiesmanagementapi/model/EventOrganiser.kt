package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An event organiser")
data class EventOrganiser(

  @Schema(description = "The internally-generated ID for this event organiser", example = "1")
  val id: Long,

  @Schema(description = "The code for this event organiser", example = "PRISON_STAFF")
  val code: String,

  @Schema(description = "The detailed description for this event organiser", example = "Prison staff")
  val description: String,
)
