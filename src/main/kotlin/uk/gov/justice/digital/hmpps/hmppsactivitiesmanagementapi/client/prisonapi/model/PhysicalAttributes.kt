package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Physical Attributes
 * @param sexCode Gender Code
 * @param gender Gender
 * @param raceCode Ethnicity Code
 * @param ethnicity Ethnicity
 * @param heightFeet Height in Feet
 * @param heightInches Height in Inches
 * @param heightMetres Height in Metres (to 2dp)
 * @param heightCentimetres Height in Centimetres
 * @param weightPounds Weight in Pounds
 * @param weightKilograms Weight in Kilograms
 */
data class PhysicalAttributes(

  @Schema(example = "M", description = "Gender Code")
  @JsonProperty("sexCode") val sexCode: String?,

  @Schema(example = "Male", description = "Gender")
  @JsonProperty("gender") val gender: String,

  @Schema(example = "W1", description = "Ethnicity Code")
  @JsonProperty("raceCode") val raceCode: String?,

  @Schema(example = "White: Eng./Welsh/Scot./N.Irish/British", description = "Ethnicity")
  @JsonProperty("ethnicity") val ethnicity: String?,

  @Schema(example = "5", description = "Height in Feet")
  @JsonProperty("heightFeet") val heightFeet: Int?,

  @Schema(example = "60", description = "Height in Inches")
  @JsonProperty("heightInches") val heightInches: Int?,

  @Schema(example = "1.76", description = "Height in Metres (to 2dp)")
  @JsonProperty("heightMetres") val heightMetres: java.math.BigDecimal?,

  @Schema(example = "176", description = "Height in Centimetres")
  @JsonProperty("heightCentimetres") val heightCentimetres: Int?,

  @Schema(example = "50", description = "Weight in Pounds")
  @JsonProperty("weightPounds") val weightPounds: Int?,

  @Schema(example = "67", description = "Weight in Kilograms")
  @JsonProperty("weightKilograms") val weightKilograms: Int?
)
