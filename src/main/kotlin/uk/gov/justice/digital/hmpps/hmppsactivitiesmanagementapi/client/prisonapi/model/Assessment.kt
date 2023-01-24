package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Assessment
 * @param bookingId Booking number
 * @param offenderNo Offender number (e.g. NOMS Number).
 * @param classificationCode Classification code
 * @param classification Classification description
 * @param assessmentCode Identifies the type of assessment
 * @param assessmentDescription Assessment description
 * @param cellSharingAlertFlag Indicates the presence of a cell sharing alert
 * @param assessmentDate Date assessment was created
 * @param nextReviewDate Date of next review
 * @param approvalDate Date of assessment approval
 * @param assessmentAgencyId The assessment creation agency id
 * @param assessmentStatus The status of the assessment
 * @param assessmentSeq Sequence number of assessment within booking
 * @param assessmentComment Comment from assessor
 * @param assessorId Staff member who made the assessment
 * @param assessorUser Username who made the assessment
 */
data class Assessment(

  @Schema(example = "123456", description = "Booking number")
  @JsonProperty("bookingId") val bookingId: Long,

  @Schema(example = "GV09876N", description = "Offender number (e.g. NOMS Number).")
  @JsonProperty("offenderNo") val offenderNo: String?,

  @Schema(example = "C", description = "Classification code")
  @JsonProperty("classificationCode") val classificationCode: String,

  @Schema(example = "Cat C", description = "Classification description")
  @JsonProperty("classification") val classification: String?,

  @Schema(example = "CATEGORY", description = "Identifies the type of assessment")
  @JsonProperty("assessmentCode") val assessmentCode: String,

  @Schema(example = "Categorisation", description = "Assessment description")
  @JsonProperty("assessmentDescription") val assessmentDescription: String,

  @Schema(example = "null", description = "Indicates the presence of a cell sharing alert")
  @JsonProperty("cellSharingAlertFlag") val cellSharingAlertFlag: Boolean,

  @Valid
  @Schema(example = "Sun Feb 11 00:00:00 GMT 2018", description = "Date assessment was created")
  @JsonProperty("assessmentDate") val assessmentDate: java.time.LocalDate,

  @Valid
  @Schema(example = "Sun Feb 11 00:00:00 GMT 2018", description = "Date of next review")
  @JsonProperty("nextReviewDate") val nextReviewDate: java.time.LocalDate,

  @Valid
  @Schema(example = "Sun Feb 11 00:00:00 GMT 2018", description = "Date of assessment approval")
  @JsonProperty("approvalDate") val approvalDate: java.time.LocalDate? = null,

  @Schema(example = "MDI", description = "The assessment creation agency id")
  @JsonProperty("assessmentAgencyId") val assessmentAgencyId: String? = null,

  @Schema(example = "A", description = "The status of the assessment")
  @JsonProperty("assessmentStatus") val assessmentStatus: AssessmentStatus? = null,

  @Schema(example = "1", description = "Sequence number of assessment within booking")
  @JsonProperty("assessmentSeq") val assessmentSeq: Int? = null,

  @Schema(example = "Comment details", description = "Comment from assessor")
  @JsonProperty("assessmentComment") val assessmentComment: String? = null,

  @Schema(example = "130000", description = "Staff member who made the assessment")
  @JsonProperty("assessorId") val assessorId: Long? = null,

  @Schema(example = "NGK33Y", description = "Username who made the assessment")
  @JsonProperty("assessorUser") val assessorUser: String? = null
) {

  /**
   * The status of the assessment
   * Values: p,a,i
   */
  enum class AssessmentStatus(val value: String) {

    @JsonProperty("P")
    P("P"),
    @JsonProperty("A")
    A("A"),
    @JsonProperty("I")
    I("I")
  }
}
