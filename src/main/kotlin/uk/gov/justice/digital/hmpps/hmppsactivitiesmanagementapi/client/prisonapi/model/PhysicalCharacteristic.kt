package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Physical Characteristic
 * @param type Type code of physical characteristic
 * @param characteristic Type of physical characteristic
 * @param detail Detailed information about the physical characteristic
 * @param imageId Image Id Ref
 */
data class PhysicalCharacteristic(

  @Schema(example = "null", description = "Type code of physical characteristic")
  @JsonProperty("type")
  val type: String,

  @Schema(example = "null", description = "Type of physical characteristic")
  @JsonProperty("characteristic")
  val characteristic: String,

  @Schema(example = "null", description = "Detailed information about the physical characteristic")
  @JsonProperty("detail")
  val detail: String,

  @Schema(example = "null", description = "Image Id Ref")
  @JsonProperty("imageId")
  val imageId: Long? = null,
)
