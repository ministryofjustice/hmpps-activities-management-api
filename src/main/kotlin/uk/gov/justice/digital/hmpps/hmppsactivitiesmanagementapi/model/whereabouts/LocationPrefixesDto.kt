package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Sub-locations prefix response")
data class LocationPrefixesDto(
  @Schema(description = "Name of the sub-location", example = "North Landing 1")
  val subLocation: String,

  @Schema(description = "Location prefix resolved from the sub-location", example = "RSI-A-N-1-.+")
  val locationPrefix: String,
)
