package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Overriding `toAgency` field. It can be null despite being non-nullable in the API specification.
 */
data class Movement(
  @get:JsonProperty("offenderNo", required = true) val offenderNo: String,

  @get:JsonProperty("createDateTime", required = true) val createDateTime: String,

  @get:JsonProperty("fromAgency", required = true) val fromAgency: String,

  @get:JsonProperty("fromAgencyDescription", required = true) val fromAgencyDescription: String,

  @get:JsonProperty("toAgency", required = false) val toAgency: String?,

  @get:JsonProperty("toAgencyDescription", required = true) val toAgencyDescription: String,

  @get:JsonProperty("movementType", required = true) val movementType: MovementType,

  @get:JsonProperty("movementTypeDescription", required = true) val movementTypeDescription: String,

  @get:JsonProperty("directionCode", required = true) val directionCode: String,

  @get:JsonProperty("movementDate", required = true) val movementDate: java.time.LocalDate,

  @get:JsonProperty("movementTime", required = true) val movementTime: String,

  @get:JsonProperty("movementReason", required = true) val movementReason: String,

  @get:JsonProperty("fromCity") val fromCity: String? = null,

  @get:JsonProperty("toCity") val toCity: String? = null,

  @get:JsonProperty("commentText") val commentText: String? = null,
) {
  /**
   * ADM (admission), CRT (court), REL (release), TAP (temporary absence) or TRN (transfer)
   * Values: ADM,CRT,REL,TAP,TRN
   */
  enum class MovementType(val value: kotlin.String) {
    @JsonProperty("ADM")
    ADM("ADM"),

    @JsonProperty("CRT")
    CRT("CRT"),

    @JsonProperty("REL")
    REL("REL"),

    @JsonProperty("TAP")
    TAP("TAP"),

    @JsonProperty("TRN")
    TRN("TRN")
  }

  fun movementDateTime(): LocalDateTime = LocalDateTime.of(movementDate, LocalTime.parse(movementTime))
}
