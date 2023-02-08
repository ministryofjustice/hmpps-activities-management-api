package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Aliases Names and Details
 * @param firstName First Name
 * @param lastName Last name
 * @param dateOfBirth Date of birth
 * @param middleNames Middle names
 * @param gender Gender
 * @param ethnicity Ethnicity
 */
data class PrisonerAlias(
  @Schema(example = "Robert", required = true, description = "First Name")
  @get:JsonProperty("firstName", required = true) val firstName: kotlin.String,

  @Schema(example = "Lorsen", required = true, description = "Last name")
  @get:JsonProperty("lastName", required = true) val lastName: kotlin.String,

  @Schema(example = "Wed Apr 02 01:00:00 BST 1975", required = true, description = "Date of birth")
  @get:JsonProperty("dateOfBirth", required = true) val dateOfBirth: java.time.LocalDate,

  @Schema(example = "Trevor", description = "Middle names")
  @get:JsonProperty("middleNames") val middleNames: kotlin.String? = null,

  @Schema(example = "Male", description = "Gender")
  @get:JsonProperty("gender") val gender: kotlin.String? = null,

  @Schema(example = "White : Irish", description = "Ethnicity")
  @get:JsonProperty("ethnicity") val ethnicity: kotlin.String? = null
)
