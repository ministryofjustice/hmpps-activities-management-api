package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class EducationLevel(

  @Schema(example = "EDU_LEVEL", required = true, description = "Domain.")
  @JsonProperty("domain", required = true) val domain: String,

  @Schema(example = "1", required = true, description = "Education level code.")
  @JsonProperty("code", required = true) val code: String,

  @Schema(example = "Reading Measure 1.0", required = true, description = "Education level description.")
  @JsonProperty("description", required = true) val description: String,

  @Schema(example = "STL", required = true, description = "Parent code.")
  @JsonProperty("parentCode", required = true) val parentCode: String,

  @Schema(example = "Y", required = true, description = "Active flag.")
  @JsonProperty("activeFlag", required = true) val activeFlag: String,

  @Schema(example = "Reading Measure 1.0", required = true, description = "List sequence.")
  @JsonProperty("listSeq", required = true) val listSeq: Int,

  @Schema(example = "N", required = true, description = "System data flag.")
  @JsonProperty("systemDataFlag", required = true) val systemDataFlag: String,

  @Schema(example = "null", description = "List of sub codes.")
  @JsonProperty("subCodes") val subCodes: List<String>? = null

)
