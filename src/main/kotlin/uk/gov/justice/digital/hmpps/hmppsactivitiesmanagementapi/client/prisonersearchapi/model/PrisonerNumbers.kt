package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @param prisonerNumbers List of prisoner numbers to search by
 */
data class PrisonerNumbers(
  @Schema(example = "[\"A1234AA\"]", required = true, description = "List of prisoner numbers to search by")
  @get:JsonProperty("prisonerNumbers", required = true) val prisonerNumbers: kotlin.collections.List<kotlin.String>
)
