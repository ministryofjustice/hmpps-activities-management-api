package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Assigned Living Unit
 * @param agencyId Agency Id
 * @param locationId location Id
 * @param description Living Unit Desc
 * @param agencyName Name of the agency where this living unit resides
 */
data class AssignedLivingUnit(

  @Schema(example = "null", required = true, description = "Agency Id")
  @JsonProperty("agencyId", required = true) val agencyId: String,

  @Schema(example = "null", required = true, description = "location Id")
  @JsonProperty("locationId", required = true) val locationId: Long,

  @Schema(example = "null", required = true, description = "Living Unit Desc")
  @JsonProperty("description", required = true) val description: String,

  @Schema(example = "null", required = true, description = "Name of the agency where this living unit resides")
  @JsonProperty("agencyName", required = true) val agencyName: String
)
