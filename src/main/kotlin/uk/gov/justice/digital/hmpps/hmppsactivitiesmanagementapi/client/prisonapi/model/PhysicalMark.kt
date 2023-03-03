package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Physical Mark
 * @param type Type of Mark
 * @param side Left or Right Side
 * @param bodyPart Where on the body
 * @param orientation Image orientation
 * @param comment More information
 * @param imageId Image Id Ref
 */
data class PhysicalMark(

  @Schema(example = "null", description = "Type of Mark")
  @JsonProperty("type")
  val type: String,

  @Schema(example = "null", description = "Left or Right Side")
  @JsonProperty("side")
  val side: String,

  @Schema(example = "null", description = "Where on the body")
  @JsonProperty("bodyPart")
  val bodyPart: String,

  @Schema(example = "null", description = "Image orientation")
  @JsonProperty("orientation")
  val orientation: String,

  @Schema(example = "null", description = "More information")
  @JsonProperty("comment")
  val comment: String,

  @Schema(example = "null", description = "Image Id Ref")
  @JsonProperty("imageId")
  val imageId: Long? = null,
)
