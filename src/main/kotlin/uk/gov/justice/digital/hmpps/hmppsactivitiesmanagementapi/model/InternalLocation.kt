package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "An internal NOMIS location for an activity to take place")
data class InternalLocation(

  @Schema(description = "The NOMIS internal location id for this schedule", example = "98877667")
  val id: Int,

  @Schema(description = "The NOMIS internal location code for this schedule", example = "EDU-ROOM-1")
  val code: String,

  @Schema(description = "The NOMIS internal location description for this schedule", example = "Education - R1")
  val description: String,

  @Schema(description = "The optional DPS location UUID for this schedule", example = "b7602cc8-e769-4cbb-8194-62d8e655992a")
  val dpsLocationId: UUID? = null,
)
