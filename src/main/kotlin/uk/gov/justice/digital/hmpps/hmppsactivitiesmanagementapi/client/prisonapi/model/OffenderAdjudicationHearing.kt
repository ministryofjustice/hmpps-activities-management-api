package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

/**
 * Represents an adjudication hearing at the offender level.
 * @param agencyId
 * @param offenderNo Display Prisoner Number (UK is NOMS ID)
 * @param hearingId OIC Hearing ID
 * @param internalLocationId The internal location id of the hearing
 * @param hearingType Hearing Type
 * @param startTime Hearing Time
 * @param internalLocationDescription The internal location description of the hearing
 * @param eventStatus The status of the hearing, SCH, COMP or EXP
 */
data class OffenderAdjudicationHearing(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("agencyId", required = true) val agencyId: kotlin.String,

  @Schema(example = "null", required = true, description = "Display Prisoner Number (UK is NOMS ID)")
  @get:JsonProperty("offenderNo", required = true) val offenderNo: kotlin.String,

  @Schema(example = "1985937", required = true, description = "OIC Hearing ID")
  @get:JsonProperty("hearingId", required = true) val hearingId: kotlin.Long,

  @Schema(example = "789448", required = true, description = "The internal location id of the hearing")
  @get:JsonProperty("internalLocationId", required = true) val internalLocationId: kotlin.Long,

  @Schema(example = "Governor's Hearing Adult", description = "Hearing Type")
  @get:JsonProperty("hearingType") val hearingType: kotlin.String? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Hearing Time")
  @get:JsonProperty("startTime") val startTime: kotlin.String? = null,

  @Schema(example = "PVI-RES-MCASU-ADJUD", description = "The internal location description of the hearing")
  @get:JsonProperty("internalLocationDescription") val internalLocationDescription: kotlin.String? = null,

  @Schema(example = "COMP", description = "The status of the hearing, SCH, COMP or EXP")
  @get:JsonProperty("eventStatus") val eventStatus: kotlin.String? = null,
) {

}

