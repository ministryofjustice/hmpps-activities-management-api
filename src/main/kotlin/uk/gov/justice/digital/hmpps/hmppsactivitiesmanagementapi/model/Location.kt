package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A location for an activity to take place")
data class Location(

  @Schema(description = "The internal ID for this location", example = "123456")
  val id: Long,

  @Schema(description = "The location code", example = "EDU-ROOM-1")
  val code: String,

  @Schema(description = "The description for this activity category", example = "Education - R1")
  val description: String
)
