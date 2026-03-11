package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Bulk request for resolving location prefixes")
data class LocationPrefixesRequest(
  @field:NotEmpty(message = "At least one sub-location must be provided")
  @Schema(description = "List of sub-locations to resolve location prefixes", example = "[\"North Landing 1\", \"North All\"]")
  val subLocations: List<String>,
)
