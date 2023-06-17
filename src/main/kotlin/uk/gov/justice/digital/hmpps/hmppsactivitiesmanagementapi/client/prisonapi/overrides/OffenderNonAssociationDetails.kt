package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// Overridden because it implements overridden class, `OffenderNonAssociationDetail`
data class OffenderNonAssociationDetails(
  @Schema(example = "G9109UD", required = true, description = "The offenders number")
  @get:JsonProperty("offenderNo", required = true) val offenderNo: String,

  @Schema(example = "Fred", required = true, description = "The offenders first name")
  @get:JsonProperty("firstName", required = true) val firstName: String,

  @Schema(example = "Bloggs", required = true, description = "The offenders last name")
  @get:JsonProperty("lastName", required = true) val lastName: String,

  @Schema(example = "Moorland (HMP & YOI)", required = true, description = "Description of the agency (e.g. prison) the offender is assigned to.")
  @get:JsonProperty("agencyDescription", required = true) val agencyDescription: String,

  @Schema(example = "MDI-1-1-3", required = true, description = "Description of living unit (e.g. cell) the offender is assigned to.")
  @get:JsonProperty("assignedLivingUnitDescription", required = true) val assignedLivingUnitDescription: String,

  @Schema(example = "123", required = true, description = "Id of living unit (e.g. cell) the offender is assigned to.")
  @get:JsonProperty("assignedLivingUnitId", required = true) val assignedLivingUnitId: Long,

  @Schema(example = "null", description = "Offender non-association details")
  @get:JsonProperty("nonAssociations") val nonAssociations: List<OffenderNonAssociationDetail>? = null
)
