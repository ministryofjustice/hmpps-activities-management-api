package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Represents court hearings for an offender booking.
 * @param hearings
 */
data class CourtHearings(

  @Valid
  @Schema(example = "null", description = "")
  @JsonProperty("hearings")
  val hearings: List<CourtHearing>? = null,
)
