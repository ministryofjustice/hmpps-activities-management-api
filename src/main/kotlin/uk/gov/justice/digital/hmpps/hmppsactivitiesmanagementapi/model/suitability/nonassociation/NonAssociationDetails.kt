package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Prisoner non-association details")
data class NonAssociationDetails(
  @Schema(example = "true", description = "Is allocated to this activity?")
  val allocated: Boolean,

  @Schema(example = "GANG_RELATED", required = true, description = "The reason code")
  val reasonCode: String,

  @Schema(example = "Gang related", required = true, description = "The reason description")
  val reasonDescription: String,

  @Schema(example = "VICTIM", required = true, description = "The role code")
  val roleCode: String,

  @Schema(example = "VICTIM", required = true, description = "The role description")
  val roleDescription: String,

  @Schema(example = "WING", required = true, description = "The restriction type")
  val restrictionType: String,

  @Schema(example = "Cell, landing and wing", required = true, description = "The restriction type description")
  val restrictionTypeDescription: String,

  @Schema(required = true, description = "")
  val otherPrisonerDetails: OtherPrisonerDetails,

  @Schema(example = "2021-07-05T10:35:17", required = true, description = "Date and time the non-association was updated. In Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val whenUpdated: LocalDateTime,

  @Schema(example = "Violent acts", description = "Additional free text comments related to the non-association.")
  val comments: String? = null,
)
