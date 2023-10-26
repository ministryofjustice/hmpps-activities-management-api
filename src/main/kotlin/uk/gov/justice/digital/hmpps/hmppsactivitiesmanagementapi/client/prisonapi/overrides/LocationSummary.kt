package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * The GET /api/agencies/{agencyId}/eventLocationsBooked Prison API endpoint's documentation shows it returning the full
 * Location model. It however only returns locationId, description and userDescription from that model. Using the full
 * Location model therefore causes deserialization exceptions. This LocationSummary model is used instead to prevent that.
 */
data class LocationSummary(
    @Schema(example = "null", required = true, description = "Location identifier.")
    @get:JsonProperty("locationId", required = true) val locationId: Long,

    @Schema(example = "null", required = true, description = "Location description.")
    @get:JsonProperty("description", required = true) val description: String,

    @Schema(example = "null", description = "User-friendly location description.")
    @get:JsonProperty("userDescription") val userDescription: String? = null,
)
