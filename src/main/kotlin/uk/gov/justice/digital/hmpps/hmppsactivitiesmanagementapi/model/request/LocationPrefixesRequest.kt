package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Bulk request for resolving location prefixes")
data class LocationPrefixesRequest(
  @Schema(description = "List of sub-locations to resolve location prefixes", example = "[\"North Landing 1\", \"North All\"]")
  val subLocations: List<String> = emptyList(),
)
