package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * Offence History Item
 * @param bookingId Prisoner booking id
 * @param offenceDate Date the offence took place
 * @param offenceDescription Description associated with the offence code
 * @param offenceCode Reference Code
 * @param statuteCode Statute code
 * @param mostSerious Identifies the main offence per booking
 * @param offenceRangeDate End date if range the offence was believed to have taken place
 * @param primaryResultCode Primary result code
 * @param secondaryResultCode Secondary result code
 * @param primaryResultDescription Description for Primary result
 * @param secondaryResultDescription Description for Secondary result
 * @param primaryResultConviction Conviction flag for Primary result
 * @param secondaryResultConviction Conviction flag for Secondary result
 * @param courtDate Latest court date associated with the offence
 * @param caseId Court case id
 */
data class OffenceHistoryDetail(

  @Schema(example = "1123456", description = "Prisoner booking id")
  @JsonProperty("bookingId") val bookingId: Long,

  @Valid
  @Schema(example = "Sat Feb 10 00:00:00 GMT 2018", description = "Date the offence took place")
  @JsonProperty("offenceDate") val offenceDate: java.time.LocalDate,

  @Schema(
    example = "Commit an act / series of acts with intent to pervert the course of public justice",
    required = true,
    description = "Description associated with the offence code"
  )
  @JsonProperty("offenceDescription") val offenceDescription: String,

  @Schema(example = "RR84070", description = "Reference Code")
  @JsonProperty("offenceCode") val offenceCode: String,

  @Schema(example = "RR84", description = "Statute code")
  @JsonProperty("statuteCode") val statuteCode: String,

  @Schema(example = "null", description = "Identifies the main offence per booking")
  @JsonProperty("mostSerious") val mostSerious: Boolean,

  @Valid
  @Schema(
    example = "Sat Mar 10 00:00:00 GMT 2018",
    description = "End date if range the offence was believed to have taken place"
  )
  @JsonProperty("offenceRangeDate") val offenceRangeDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Primary result code ")
  @JsonProperty("primaryResultCode") val primaryResultCode: String? = null,

  @Schema(example = "null", description = "Secondary result code")
  @JsonProperty("secondaryResultCode") val secondaryResultCode: String? = null,

  @Schema(example = "null", description = "Description for Primary result")
  @JsonProperty("primaryResultDescription") val primaryResultDescription: String? = null,

  @Schema(example = "null", description = "Description for Secondary result")
  @JsonProperty("secondaryResultDescription") val secondaryResultDescription: String? = null,

  @Schema(example = "null", description = "Conviction flag for Primary result ")
  @JsonProperty("primaryResultConviction") val primaryResultConviction: Boolean? = null,

  @Schema(example = "null", description = "Conviction flag for Secondary result ")
  @JsonProperty("secondaryResultConviction") val secondaryResultConviction: Boolean? = null,

  @Valid
  @Schema(example = "Sat Feb 10 00:00:00 GMT 2018", description = "Latest court date associated with the offence")
  @JsonProperty("courtDate") val courtDate: java.time.LocalDate? = null,

  @Schema(example = "100", description = "Court case id")
  @JsonProperty("caseId") val caseId: Long? = null
)
