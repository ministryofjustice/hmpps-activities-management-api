package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// Overridden because the `assignedLivingUnitDescription` and `assignedLivingUnitId` fields can be null despite the
// required=true option.
data class OffenderNonAssociation(
  @Schema(example = "G0135GA", required = true, description = "The offenders number")
  @get:JsonProperty("offenderNo", required = true) val offenderNo: String,

  @Schema(example = "Joseph", required = true, description = "The offenders first name")
  @get:JsonProperty("firstName", required = true) val firstName: String,

  @Schema(example = "Bloggs", required = true, description = "The offenders last name")
  @get:JsonProperty("lastName", required = true) val lastName: String,

  @Schema(example = "PER", required = true, description = "The non-association reason code")
  @get:JsonProperty("reasonCode", required = true) val reasonCode: String,

  @Schema(example = "Perpetrator", required = true, description = "The non-association reason description")
  @get:JsonProperty("reasonDescription", required = true) val reasonDescription: String,

  @Schema(example = "Pentonville (PVI)", required = true, description = "Description of the agency (e.g. prison) the offender is assigned to.")
  @get:JsonProperty("agencyDescription", required = true) val agencyDescription: String,

  @Schema(example = "PVI-1-2-4", required = true, description = "Description of living unit (e.g. cell) the offender is assigned to.")
  @get:JsonProperty("assignedLivingUnitDescription", required = true) val assignedLivingUnitDescription: String? = null,

  @Schema(example = "123", required = true, description = "Id of living unit (e.g. cell) the offender is assigned to.")
  @get:JsonProperty("assignedLivingUnitId", required = true) val assignedLivingUnitId: Long? = null,
)
