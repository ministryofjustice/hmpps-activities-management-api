package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * This is overriding the generated ReferenceCode model because domain is marked as mandatory even though it can be null.
 *
 * The offending prison-api endpoint "/api/reference-domains/scheduleReasons"
 */
data class ReferenceCode(

  @Schema(example = "TASK_TYPE", description = "Reference data item domain.")
  @get:JsonProperty("domain")
  val domain: String? = null,

  @Schema(example = "MISC", required = true, description = "Reference data item code.")
  @get:JsonProperty("code", required = true)
  val code: String,

  @Schema(example = "Some description", required = true, description = "Reference data item description.")
  @get:JsonProperty("description", required = true)
  val description: String,

  @Schema(example = "Y", required = true, description = "Reference data item active indicator flag.")
  @get:JsonProperty("activeFlag", required = true)
  val activeFlag: String,

  @Schema(example = "TASK_TYPE", description = "Parent reference data item domain.")
  @get:JsonProperty("parentDomain")
  val parentDomain: String? = null,

  @Schema(example = "MIGRATION", description = "Parent reference data item code.")
  @get:JsonProperty("parentCode")
  val parentCode: String? = null,

  @Schema(example = "1", description = "List Sequence")
  @get:JsonProperty("listSeq")
  val listSeq: Int? = null,

  @Schema(example = "Y", description = "System Data Flag")
  @get:JsonProperty("systemDataFlag")
  val systemDataFlag: String? = null,

  @Schema(example = "Fri Mar 09 00:00:00 GMT 2018", description = "Expired Date")
  @get:JsonProperty("expiredDate")
  val expiredDate: LocalDate? = null,

  @Schema(example = "null", description = "List of subordinate reference data items associated with this reference data item. Not returned by default")
  @get:JsonProperty("subCodes")
  val subCodes: List<ReferenceCode>? = null,
) {
  fun isActive() = this.activeFlag == "Y"
}
