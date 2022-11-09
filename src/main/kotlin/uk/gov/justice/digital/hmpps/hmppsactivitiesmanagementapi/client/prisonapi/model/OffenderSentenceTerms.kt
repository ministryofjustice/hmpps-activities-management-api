package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * Offender Sentence terms details for booking id
 * @param bookingId Offender booking id.
 * @param sentenceSequence Sentence number within booking id.
 * @param termSequence Sentence term number within sentence.
 * @param startDate Start date of sentence term.
 * @param lifeSentence Whether this is a life sentence.
 * @param caseId Court case id
 * @param fineAmount Fine amount.
 * @param sentenceTermCode Sentence term code.
 * @param lineSeq Sentence line number
 * @param sentenceStartDate Sentence start date
 * @param consecutiveTo Sentence number which this sentence follows if consecutive, otherwise concurrent.
 * @param sentenceType Sentence type, using reference data from table SENTENCE_CALC_TYPES.
 * @param sentenceTypeDescription Sentence type description.
 * @param years Sentence length years.
 * @param months Sentence length months.
 * @param weeks Sentence length weeks.
 * @param days Sentence length days.
 */
data class OffenderSentenceTerms(

  @Schema(example = "1132400", description = "Offender booking id.")
  @JsonProperty("bookingId") val bookingId: Long,

  @Schema(example = "2", description = "Sentence number within booking id.")
  @JsonProperty("sentenceSequence") val sentenceSequence: Int,

  @Schema(example = "1", description = "Sentence term number within sentence.")
  @JsonProperty("termSequence") val termSequence: Int,

  @Valid
  @Schema(example = "Mon Dec 31 00:00:00 GMT 2018", description = "Start date of sentence term.")
  @JsonProperty("startDate") val startDate: java.time.LocalDate,

  @Schema(example = "null", description = "Whether this is a life sentence.")
  @JsonProperty("lifeSentence") val lifeSentence: Boolean,

  @Schema(example = "null", description = "Court case id")
  @JsonProperty("caseId") val caseId: String,

  @Schema(example = "null", description = "Fine amount.")
  @JsonProperty("fineAmount") val fineAmount: Double,

  @Schema(example = "IMP", description = "Sentence term code.")
  @JsonProperty("sentenceTermCode") val sentenceTermCode: String,

  @Schema(example = "1", description = "Sentence line number")
  @JsonProperty("lineSeq") val lineSeq: Long,

  @Valid
  @Schema(example = "Mon Dec 31 00:00:00 GMT 2018", description = "Sentence start date")
  @JsonProperty("sentenceStartDate") val sentenceStartDate: java.time.LocalDate,

  @Schema(
    example = "2",
    description = "Sentence number which this sentence follows if consecutive, otherwise concurrent."
  )
  @JsonProperty("consecutiveTo") val consecutiveTo: Int? = null,

  @Schema(example = "2", description = "Sentence type, using reference data from table SENTENCE_CALC_TYPES.")
  @JsonProperty("sentenceType") val sentenceType: String? = null,

  @Schema(example = "2", description = "Sentence type description.")
  @JsonProperty("sentenceTypeDescription") val sentenceTypeDescription: String? = null,

  @Schema(example = "null", description = "Sentence length years.")
  @JsonProperty("years") val years: Int? = null,

  @Schema(example = "null", description = "Sentence length months.")
  @JsonProperty("months") val months: Int? = null,

  @Schema(example = "null", description = "Sentence length weeks.")
  @JsonProperty("weeks") val weeks: Int? = null,

  @Schema(example = "null", description = "Sentence length days.")
  @JsonProperty("days") val days: Int? = null
)
