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

  @Schema(example = "WING", required = true, description = "The non-association type code")
  @get:JsonProperty("typeCode", required = true)
  val typeCode: String,

  @Schema(example = "Do Not Locate on Same Wing", required = true, description = "The non-association type description")
  @get:JsonProperty("typeDescription", required = true)
  val typeDescription: String,

  @Schema(example = "2021-07-05T10:35:17", required = true, description = "Date and time the mom-association is effective from. In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.")
  @get:JsonProperty("effectiveDate", required = true)
  val effectiveDate: LocalDateTime,

  @Schema(example = "2021-07-05T10:35:17", description = "Date and time the mom-association expires. In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.")
  @get:JsonProperty("expiryDate")
  val expiryDate: LocalDateTime? = null,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("offenderNonAssociation", required = true)
  val offenderNonAssociation: OffenderNonAssociation,

  @Schema(example = "null", description = "The person who authorised the non-association (free text).")
  @get:JsonProperty("authorisedBy")
  val authorisedBy: String? = null,

  @Schema(example = "null", description = "Additional free text comments related to the non-association.")
  @get:JsonProperty("comments")
  val comments: String? = null,
)
