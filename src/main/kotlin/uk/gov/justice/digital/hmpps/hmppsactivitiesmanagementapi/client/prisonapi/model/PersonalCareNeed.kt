package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Personal Care Need
 * @param problemType Problem Type
 * @param problemCode Problem Code
 * @param problemStatus Problem Status
 * @param problemDescription Problem Description
 * @param commentText Comment Text
 * @param startDate Start Date
 * @param endDate End Date
 */
data class PersonalCareNeed(

  @Schema(example = "MATSTAT", description = "Problem Type")
  @JsonProperty("problemType") val problemType: String? = null,

  @Schema(example = "ACCU9", description = "Problem Code")
  @JsonProperty("problemCode") val problemCode: String? = null,

  @Schema(example = "ON", description = "Problem Status")
  @JsonProperty("problemStatus") val problemStatus: String? = null,

  @Schema(example = "Preg, acc under 9mths", description = "Problem Description")
  @JsonProperty("problemDescription") val problemDescription: String? = null,

  @Schema(example = "a comment", description = "Comment Text")
  @JsonProperty("commentText") val commentText: String? = null,

  @Valid
  @Schema(example = "Mon Jun 21 01:00:00 BST 2010", description = "Start Date")
  @JsonProperty("startDate") val startDate: java.time.LocalDate? = null,

  @Valid
  @Schema(example = "Mon Jun 21 01:00:00 BST 2010", description = "End Date")
  @JsonProperty("endDate") val endDate: java.time.LocalDate? = null
)
