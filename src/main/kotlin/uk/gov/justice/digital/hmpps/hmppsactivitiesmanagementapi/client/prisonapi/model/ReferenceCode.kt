package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ReferenceCode(

  @Schema(example = "EDU_LEVEL", description = "Domain.")
  @JsonProperty("domain") val domain: String? = null,

  @Schema(example = "1", description = "Education level code.")
  @JsonProperty("code") val code: String? = null,

  @Schema(example = "Reading Measure 1.0", description = "Education level description.")
  @JsonProperty("description") val description: String? = null,

  @Schema(example = "STL", description = "Parent code.")
  @JsonProperty("parentCode") val parentCode: String? = null,

  @Schema(example = "Y", description = "Active flag.")
  @JsonProperty("activeFlag") val activeFlag: String? = null,

  @Schema(example = "Reading Measure 1.0", description = "List sequence.")
  @JsonProperty("listSeq") val listSeq: Int? = null,

  @Schema(example = "N", description = "System data flag.")
  @JsonProperty("systemDataFlag") val systemDataFlag: String? = null,

  @Schema(example = "null", description = "List of sub codes.")
  @JsonProperty("subCodes") val subCodes: List<String>? = null

)
