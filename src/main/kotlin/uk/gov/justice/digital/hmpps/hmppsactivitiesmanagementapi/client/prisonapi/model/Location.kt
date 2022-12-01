package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Location Details
 * @param locationId Location identifier.
 * @param locationType Location type.
 * @param description Location description.
 * @param agencyId Identifier of Agency this location is associated with.
 * @param locationUsage What events this room can be used for.
 * @param parentLocationId Identifier of this location's parent location.
 * @param currentOccupancy Current occupancy of location.
 * @param locationPrefix Location prefix. Defines search prefix that will constrain search to this location and its subordinate locations.
 * @param operationalCapacity Operational capacity of the location.
 * @param userDescription User-friendly location description.
 * @param internalLocationCode
 */
data class Location(

  @Schema(example = "null", required = true, description = "Location identifier.")
  @JsonProperty("locationId", required = true) val locationId: Long,

  @Schema(example = "null", required = true, description = "Location type.")
  @JsonProperty("locationType", required = true) val locationType: String,

  @Schema(example = "null", required = true, description = "Location description.")
  @JsonProperty("description", required = true) val description: String,

  @Schema(example = "null", required = true, description = "Identifier of Agency this location is associated with.")
  @JsonProperty("agencyId", required = true) val agencyId: String,

  @Schema(example = "null", description = "What events this room can be used for.")
  @JsonProperty("locationUsage") val locationUsage: String? = null,

  @Schema(example = "null", description = "Identifier of this location's parent location.")
  @JsonProperty("parentLocationId") val parentLocationId: Long? = null,

  @Schema(example = "null", description = "Current occupancy of location.")
  @JsonProperty("currentOccupancy") val currentOccupancy: Int? = null,

  @Schema(
    example = "null",
    description = "Location prefix. Defines search prefix that will constrain search to this location and its subordinate locations."
  )
  @JsonProperty("locationPrefix") val locationPrefix: String? = null,

  @Schema(example = "null", description = "Operational capacity of the location.")
  @JsonProperty("operationalCapacity") val operationalCapacity: Int? = null,

  @Schema(example = "null", description = "User-friendly location description.")
  @JsonProperty("userDescription") val userDescription: String? = null,

  @Schema(example = "null", description = "")
  @JsonProperty("internalLocationCode") val internalLocationCode: String? = null
)
