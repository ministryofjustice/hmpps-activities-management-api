package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Prisoner non-association details")
data class NonAssociationDetails(
  @Schema(example = "VIC", required = true, description = "The non-association reason code")
  @get:JsonProperty("reasonCode", required = true)
  val reasonCode: String,

  @Schema(example = "Victim", required = true, description = "The non-association reason description")
  @get:JsonProperty("reasonDescription", required = true)
  val reasonDescription: String,

  @Schema(required = true, description = "")
  @get:JsonProperty("otherPrisonerDetails", required = true)
  val otherPrisonerDetails: OtherPrisonerDetails,

  @Schema(example = "2021-07-05T10:35:17", required = true, description = "Date and time the non-association is effective from. In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.")
  @get:JsonProperty("whenCreated", required = true)
  val whenCreated: LocalDateTime,

  @Schema(example = "null", description = "Additional free text comments related to the non-association.")
  @get:JsonProperty("comments")
  val comments: String? = null,
)
