package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An internal NOMIS location for an activity to take place")
data class InternalLocation(

  @Schema(description = "The NOMIS internal location id for this schedule", example = "98877667")
  val id: Int,

  @Schema(description = "The NOMIS internal location code for this schedule", example = "EDU-ROOM-1")
  val code: String,

  @Schema(description = "The NOMIS internal location description for this schedule", example = "Education - R1")
  val description: String
)
