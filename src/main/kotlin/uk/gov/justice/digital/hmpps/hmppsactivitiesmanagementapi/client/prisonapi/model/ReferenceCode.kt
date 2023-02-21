package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * Reference Code
 * @param domain Reference data item domain.
 * @param code Reference data item code.
 * @param description Reference data item description.
 * @param activeFlag Reference data item active indicator flag.
 * @param parentDomain Parent reference data item domain.
 * @param parentCode Parent reference data item code.
 * @param listSeq List Sequence
 * @param systemDataFlag System Data Flag
 * @param expiredDate Expired Date
 * @param subCodes List of subordinate reference data items associated with this reference data item. Not returned by default
 */
data class ReferenceCode(

  @Schema(example = "TASK_TYPE", required = true, description = "Reference data item domain.")
  @get:JsonProperty("domain", required = true) val domain: String,

  @Schema(example = "MISC", required = true, description = "Reference data item code.")
  @get:JsonProperty("code", required = true) val code: String,

  @Schema(example = "Some description", required = true, description = "Reference data item description.")
  @get:JsonProperty("description", required = true) val description: String,

  @Schema(example = "Y", required = true, description = "Reference data item active indicator flag.")
  @get:JsonProperty("activeFlag", required = true) val activeFlag: String,

  @Schema(example = "TASK_TYPE", description = "Parent reference data item domain.")
  @get:JsonProperty("parentDomain") val parentDomain: String? = null,

  @Schema(example = "MIGRATION", description = "Parent reference data item code.")
  @get:JsonProperty("parentCode") val parentCode: String? = null,

  @Schema(example = "1", description = "List Sequence")
  @get:JsonProperty("listSeq") val listSeq: Int? = null,

  @Schema(example = "Y", description = "System Data Flag")
  @get:JsonProperty("systemDataFlag") val systemDataFlag: String? = null,

  @Schema(example = "Fri Mar 09 00:00:00 GMT 2018", description = "Expired Date")
  @get:JsonProperty("expiredDate") val expiredDate: LocalDate? = null,

  @Schema(example = "null", description = "List of subordinate reference data items associated with this reference data item. Not returned by default")
  @get:JsonProperty("subCodes") val subCodes: List<ReferenceCode>? = null
)
