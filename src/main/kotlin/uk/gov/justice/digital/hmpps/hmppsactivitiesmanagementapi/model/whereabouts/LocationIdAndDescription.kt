package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A minimal representation of a NOMIS agency internal location.")
data class LocationIdAndDescription(
  @Schema(description = "The NOMIS agency internal location identifier of the location", example = "12345")
  val locationId: Long,

  @Schema(description = "The NOMIS description of the location", example = "VCC Room 16")
  val description: String
)
