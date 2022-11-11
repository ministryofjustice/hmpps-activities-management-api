package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Telephone Details
 * @param number Telephone number
 * @param type Telephone type
 * @param phoneId Phone Id
 * @param ext Telephone extension number
 */
data class Telephone(

  @Schema(example = "0114 2345678", description = "Telephone number")
  @JsonProperty("number") val number: String,

  @Schema(example = "TEL", description = "Telephone type")
  @JsonProperty("type") val type: String,

  @Schema(example = "2234232", description = "Phone Id")
  @JsonProperty("phoneId") val phoneId: Long? = null,

  @Schema(example = "123", description = "Telephone extension number")
  @JsonProperty("ext") val ext: String? = null
)
