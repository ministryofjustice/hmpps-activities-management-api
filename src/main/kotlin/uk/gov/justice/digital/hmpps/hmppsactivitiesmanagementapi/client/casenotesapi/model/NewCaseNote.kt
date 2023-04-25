package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Create a Case Note
 * @param locationId Location where case note was made, if blank it will be looked up in Nomis
 * @param type Type of case note
 * @param subType Sub Type of case note
 * @param occurrenceDateTime Occurrence time of case note
 * @param text Text of case note
 */
data class NewCaseNote(

  @Schema(example = "MDI", required = true, description = "Location where case note was made, if blank it will be looked up in Nomis")
  @get:JsonProperty("locationId", required = true)
  val locationId: kotlin.String,

  @Schema(example = "GEN", required = true, description = "Type of case note")
  @get:JsonProperty("type", required = true)
  val type: kotlin.String,

  @Schema(example = "OBS", required = true, description = "Sub Type of case note")
  @get:JsonProperty("subType", required = true)
  val subType: kotlin.String,

  @Schema(example = "null", required = true, description = "Occurrence time of case note")
  @get:JsonProperty("occurrenceDateTime", required = true)
  val occurrenceDateTime: java.time.LocalDateTime?,

  @Schema(example = "This is a case note message", required = true, description = "Text of case note")
  @get:JsonProperty("text", required = true)
  val text: kotlin.String,
)
