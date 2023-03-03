package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Case Load
 * @param caseLoadId Case Load ID
 * @param description Full description of the case load
 * @param type Type of case load. Note: Reference Code CSLD_TYPE
 * @param currentlyActive Indicates that this caseload in the context of a staff member is the current active
 * @param caseloadFunction Functional Use of the case load
 */
data class CaseLoad(
  @Schema(example = "MDI", required = true, description = "Case Load ID")
  @get:JsonProperty("caseLoadId", required = true)
  val caseLoadId: String,

  @Schema(example = "Moorland Closed (HMP & YOI)", required = true, description = "Full description of the case load")
  @get:JsonProperty("description", required = true)
  val description: String,

  @Schema(example = "INST", required = true, description = "Type of case load. Note: Reference Code CSLD_TYPE")
  @get:JsonProperty("type", required = true)
  val type: Type,

  @Schema(example = "false", required = true, description = "Indicates that this caseload in the context of a staff member is the current active")
  @get:JsonProperty("currentlyActive", required = true)
  val currentlyActive: Boolean,

  @Schema(example = "GENERAL", description = "Functional Use of the case load")
  @get:JsonProperty("caseloadFunction")
  val caseloadFunction: CaseloadFunction? = null,
) {
  /**
   * Type of case load. Note: Reference Code CSLD_TYPE
   * Values: COMM,INST,APP,DUMMY
   */
  enum class Type(val value: String) {
    @JsonProperty("COMM")
    COMM("COMM"),

    @JsonProperty("INST")
    INST("INST"),

    @JsonProperty("APP")
    APP("APP"),

    @JsonProperty("DUMMY")
    DUMMY("DUMMY"),
  }

  /**
   * Functional Use of the case load
   * Values: GENERAL,ADMIN
   */
  enum class CaseloadFunction(val value: String) {
    @JsonProperty("GENERAL")
    GENERAL("GENERAL"),

    @JsonProperty("ADMIN")
    ADMIN("ADMIN"),
  }
}
