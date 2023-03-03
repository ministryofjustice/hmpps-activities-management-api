package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Location prefix response")
data class LocationPrefixDto(
  @Schema(description = "Location prefix translated from group name", example = "MDI-1-")
  val locationPrefix: String,
)
