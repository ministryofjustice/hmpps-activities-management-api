package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Incentive level
 * @param description description
 * @param code code
 */
data class IncentiveLevel(
  @Schema(example = "Standard", required = true, description = "description")
  @get:JsonProperty("description", required = true) val description: kotlin.String,

  @Schema(example = "STD", description = "code")
  @get:JsonProperty("code") val code: kotlin.String? = null
)
