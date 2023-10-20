package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Non-association prisoner details")
data class OtherPrisonerDetails(
  @Schema(example = "G0135GA", required = true, description = "The prisoners number")
  @get:JsonProperty("prisonerNumber", required = true)
  val prisonerNumber: String,

  @Schema(example = "Joseph", required = true, description = "The prisoners first name")
  @get:JsonProperty("firstName", required = true)
  val firstName: String,

  @Schema(example = "Bloggs", required = true, description = "The prisoners last name")
  @get:JsonProperty("lastName", required = true)
  val lastName: String,

  @Schema(example = "PVI-1-2-4", required = true, description = "Description of living unit (e.g. cell) the offender is assigned to.")
  @get:JsonProperty("cellLocation", required = true)
  val cellLocation: String? = null,
)
