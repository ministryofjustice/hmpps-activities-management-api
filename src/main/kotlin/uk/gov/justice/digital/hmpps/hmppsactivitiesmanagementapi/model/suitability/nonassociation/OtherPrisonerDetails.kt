package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Non-association prisoner details")
data class OtherPrisonerDetails(
  @Schema(example = "G0135GA", required = true, description = "The prisoners number")
  val prisonerNumber: String,

  @Schema(example = "Joseph", required = true, description = "The prisoners first name")
  val firstName: String,

  @Schema(example = "Bloggs", required = true, description = "The prisoners last name")
  val lastName: String,

  @Schema(example = "PVI-1-2-4", required = true, description = "Description of living unit (e.g. cell) the offender is assigned to.")
  val cellLocation: String? = null,
)
