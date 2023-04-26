package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Case Note
 * @param caseNoteId Case Note Id (unique)
 * @param offenderIdentifier Offender Unique Identifier
 * @param type Case Note Type
 * @param typeDescription Case Note Type Description
 * @param subType Case Note Sub Type
 * @param subTypeDescription Case Note Sub Type Description
 * @param source Source Type
 * @param creationDateTime Date and Time of Case Note creation
 * @param occurrenceDateTime Date and Time of when case note contact with offender was made
 * @param authorName Full name of case note author
 * @param authorUserId User Id of case note author - staffId for nomis users, userId for auth users
 * @param text Case Note Text
 * @param eventId Delius number representation of the case note id - will be negative for sensitive case note types
 * @param sensitive Sensitive Note
 * @param amendments Ordered list of amendments to the case note (oldest first)
 * @param locationId Location Id representing where Case Note was made.
 */
data class CaseNote(

  @Schema(example = "12311312", required = true, description = "Case Note Id (unique)")
  @get:JsonProperty("caseNoteId", required = true)
  val caseNoteId: kotlin.String,

  @Schema(example = "A1234AA", required = true, description = "Offender Unique Identifier")
  @get:JsonProperty("offenderIdentifier", required = true)
  val offenderIdentifier: kotlin.String,

  @Schema(example = "KA", required = true, description = "Case Note Type")
  @get:JsonProperty("type", required = true)
  val type: kotlin.String,

  @Schema(example = "Key Worker", required = true, description = "Case Note Type Description")
  @get:JsonProperty("typeDescription", required = true)
  val typeDescription: kotlin.String,

  @Schema(example = "KS", required = true, description = "Case Note Sub Type")
  @get:JsonProperty("subType", required = true)
  val subType: kotlin.String,

  @Schema(example = "Key Worker Session", required = true, description = "Case Note Sub Type Description")
  @get:JsonProperty("subTypeDescription", required = true)
  val subTypeDescription: kotlin.String,

  @Schema(example = "INST", required = true, description = "Source Type")
  @get:JsonProperty("source", required = true)
  val source: kotlin.String,

  @Schema(example = "null", required = true, description = "Date and Time of Case Note creation")
  @get:JsonProperty("creationDateTime", required = true)
  val creationDateTime: java.time.LocalDateTime,

  @Schema(example = "null", required = true, description = "Date and Time of when case note contact with offender was made")
  @get:JsonProperty("occurrenceDateTime", required = true)
  val occurrenceDateTime: java.time.LocalDateTime,

  @Schema(example = "John Smith", required = true, description = "Full name of case note author")
  @get:JsonProperty("authorName", required = true)
  val authorName: kotlin.String,

  @Schema(example = "12345", required = true, description = "User Id of case note author - staffId for nomis users, userId for auth users")
  @get:JsonProperty("authorUserId", required = true)
  val authorUserId: kotlin.String,

  @Schema(example = "This is some text", required = true, description = "Case Note Text")
  @get:JsonProperty("text", required = true)
  val text: kotlin.String,

  @Schema(example = "-23", required = true, description = "Delius number representation of the case note id - will be negative for sensitive case note types")
  @get:JsonProperty("eventId", required = true)
  val eventId: kotlin.Int,

  @Schema(example = "true", required = true, description = "Sensitive Note")
  @get:JsonProperty("sensitive", required = true)
  val sensitive: kotlin.Boolean,

  @Schema(example = "MDI", description = "Location Id representing where Case Note was made.")
  @get:JsonProperty("locationId")
  val locationId: kotlin.String? = null,
)
