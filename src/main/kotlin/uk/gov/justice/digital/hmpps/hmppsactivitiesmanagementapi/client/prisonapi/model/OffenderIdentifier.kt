package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Offender Identifier
 * @param type Type of offender identifier
 * @param &#x60;value&#x60; The value of the offender identifier
 * @param offenderNo The offender number for this identifier
 * @param bookingId The booking ID for this identifier
 * @param issuedAuthorityText Issuing Authority Information
 * @param issuedDate Date of issue
 * @param caseloadType Related caseload type
 */
data class OffenderIdentifier(

  @Schema(example = "PNC", description = "Type of offender identifier")
  @JsonProperty("type") val type: String,

  @Schema(example = "1231/XX/121", description = "The value of the offender identifier")
  @JsonProperty("value") val `value`: String,

  @Schema(example = "A1234AB", description = "The offender number for this identifier")
  @JsonProperty("offenderNo") val offenderNo: String? = null,

  @Schema(example = "1231223", description = "The booking ID for this identifier")
  @JsonProperty("bookingId") val bookingId: Long? = null,

  @Schema(example = "Important Auth", description = "Issuing Authority Information")
  @JsonProperty("issuedAuthorityText") val issuedAuthorityText: String? = null,

  @Valid
  @Schema(example = "Sun Jan 21 00:00:00 GMT 2018", description = "Date of issue")
  @JsonProperty("issuedDate") val issuedDate: java.time.LocalDate? = null,

  @Schema(example = "GENERAL", description = "Related caseload type")
  @JsonProperty("caseloadType") val caseloadType: String? = null
)
