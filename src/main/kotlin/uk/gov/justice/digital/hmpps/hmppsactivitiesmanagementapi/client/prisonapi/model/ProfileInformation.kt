package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Profile Information
 * @param type Type of profile information
 * @param question Profile Question
 * @param resultValue Profile Result Answer
 */
data class ProfileInformation(

  @Schema(example = "null", description = "Type of profile information")
  @JsonProperty("type")
  val type: String,

  @Schema(example = "null", description = "Profile Question")
  @JsonProperty("question")
  val question: String,

  @Schema(example = "null", description = "Profile Result Answer")
  @JsonProperty("resultValue")
  val resultValue: String,
)
