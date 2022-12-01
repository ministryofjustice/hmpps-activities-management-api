package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * Cell Locations are grouped for unlock lists as a 2 level tree.
 * The two levels are referred to as Location and Sub-Location in the digital prison services UI.
 * Each (location/sub-location) group has a name that is understood by prison officers
 * and also serves as a key to retrieve the corresponding Cell Locations and information about their occupants.
 * @param name The name of the group
 * @param key A key for the group
 * @param children The child groups of this group
 */
data class LocationGroup(

  @Schema(example = "null", required = true, description = "The name of the group")
  @JsonProperty("name", required = true) val name: String,

  @Schema(example = "null", required = true, description = "A key for the group")
  @JsonProperty("key", required = true) val key: String,

  @Valid
  @Schema(example = "null", required = true, description = "The child groups of this group")
  @JsonProperty("children", required = true) val children: List<LocationGroup>
)
