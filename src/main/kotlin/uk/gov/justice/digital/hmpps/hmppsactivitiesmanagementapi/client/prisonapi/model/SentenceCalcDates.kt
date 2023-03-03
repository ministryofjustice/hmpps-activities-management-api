package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import java.time.LocalDate

/**
 * Sentence Calculation Dates
 * @param bookingId Offender booking id.
 * @param sentenceStartDate Sentence start date.
 * @param nonDtoReleaseDateType Indicates which type of non-DTO release date is the effective release date. One of 'ARD', 'CRD', 'NPD' or 'PRRD'.
 * @param sentenceExpiryDate SED - date on which sentence expires.
 * @param automaticReleaseDate ARD - calculated automatic (unconditional) release date for offender.
 * @param conditionalReleaseDate CRD - calculated conditional release date for offender.
 * @param nonParoleDate NPD - calculated non-parole date for offender (relating to the 1991 act).
 * @param postRecallReleaseDate PRRD - calculated post-recall release date for offender.
 * @param licenceExpiryDate LED - date on which offender licence expires.
 * @param homeDetentionCurfewEligibilityDate HDCED - date on which offender will be eligible for home detention curfew.
 * @param paroleEligibilityDate PED - date on which offender is eligible for parole.
 * @param homeDetentionCurfewActualDate HDCAD - the offender's actual home detention curfew date.
 * @param actualParoleDate APD - the offender's actual parole date.
 * @param releaseOnTemporaryLicenceDate ROTL - the date on which offender will be released on temporary licence.
 * @param earlyRemovalSchemeEligibilityDate ERSED - the date on which offender will be eligible for early removal (under the Early Removal Scheme for foreign nationals).
 * @param earlyTermDate ETD - early term date for offender.
 * @param midTermDate MTD - mid term date for offender.
 * @param lateTermDate LTD - late term date for offender.
 * @param topupSupervisionExpiryDate TUSED - top-up supervision expiry date for offender.
 * @param tariffDate Date on which minimum term is reached for parole (indeterminate/life sentences).
 * @param dtoPostRecallReleaseDate DPRRD - Detention training order post recall release date
 * @param tariffEarlyRemovalSchemeEligibilityDate TERSED - Tariff early removal scheme eligibility date
 * @param effectiveSentenceEndDate Effective sentence end date
 * @param additionalDaysAwarded ADA - days added to sentence term due to adjustments.
 * @param automaticReleaseOverrideDate ARD (override) - automatic (unconditional) release override date for offender.
 * @param conditionalReleaseOverrideDate CRD (override) - conditional release override date for offender.
 * @param nonParoleOverrideDate NPD (override) - non-parole override date for offender.
 * @param postRecallReleaseOverrideDate PRRD (override) - post-recall release override date for offender.
 * @param dtoPostRecallReleaseDateOverride DPRRD (override) - detention training order post-recall release override date for offender
 * @param nonDtoReleaseDate Release date for non-DTO sentence (if applicable). This will be based on one of ARD, CRD, NPD or PRRD.
 * @param sentenceExpiryCalculatedDate SED (calculated) - date on which sentence expires. (as calculated by NOMIS)
 * @param sentenceExpiryOverrideDate SED (override) - date on which sentence expires.
 * @param licenceExpiryCalculatedDate LED (calculated) - date on which offender licence expires. (as calculated by NOMIS)
 * @param licenceExpiryOverrideDate LED (override) - date on which offender licence expires.
 * @param paroleEligibilityCalculatedDate PED (calculated) - date on which offender is eligible for parole.
 * @param paroleEligibilityOverrideDate PED (override) - date on which offender is eligible for parole.
 * @param topupSupervisionExpiryCalculatedDate TUSED (calculated) - top-up supervision expiry date for offender.
 * @param topupSupervisionExpiryOverrideDate TUSED (override) - top-up supervision expiry date for offender.
 * @param homeDetentionCurfewEligibilityCalculatedDate HDCED (calculated) - date on which offender will be eligible for home detention curfew.
 * @param homeDetentionCurfewEligibilityOverrideDate HDCED (override) - date on which offender will be eligible for home detention curfew.
 * @param confirmedReleaseDate Confirmed release date for offender.
 * @param releaseDate Confirmed, actual, approved, provisional or calculated release date for offender, according to offender release date algorithm.<h3>Algorithm</h3><ul><li>If there is a confirmed release date, the offender release date is the confirmed release date.</li><li>If there is no confirmed release date for the offender, the offender release date is either the actual parole date or the home detention curfew actual date.</li><li>If there is no confirmed release date, actual parole date or home detention curfew actual date for the offender, the release date is the later of the nonDtoReleaseDate or midTermDate value (if either or both are present)</li></ul>
 * @param topupSupervisionStartDate Top-up supervision start date for offender - calculated as licence end date + 1 day or releaseDate if licence end date not set.
 * @param homeDetentionCurfewEndDate Offender's home detention curfew end date - calculated as one day before the releaseDate.
 */
data class SentenceCalcDates(

  @Schema(example = "1234123", description = "Offender booking id.")
  @JsonProperty("bookingId")
  val bookingId: Long,

  @Valid
  @Schema(example = "Wed Feb 03 00:00:00 GMT 2010", description = "Sentence start date.")
  @JsonProperty("sentenceStartDate")
  val sentenceStartDate: LocalDate,

  @Schema(
    example = "CRD",
    required = true,
    description = "Indicates which type of non-DTO release date is the effective release date. One of 'ARD', 'CRD', 'NPD' or 'PRRD'.",
  )
  @JsonProperty("nonDtoReleaseDateType")
  val nonDtoReleaseDateType: NonDtoReleaseDateType,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "SED - date on which sentence expires.")
  @JsonProperty("sentenceExpiryDate")
  val sentenceExpiryDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "ARD - calculated automatic (unconditional) release date for offender.",
  )
  @JsonProperty("automaticReleaseDate")
  val automaticReleaseDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "CRD - calculated conditional release date for offender.",
  )
  @JsonProperty("conditionalReleaseDate")
  val conditionalReleaseDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "NPD - calculated non-parole date for offender (relating to the 1991 act).",
  )
  @JsonProperty("nonParoleDate")
  val nonParoleDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "PRRD - calculated post-recall release date for offender.",
  )
  @JsonProperty("postRecallReleaseDate")
  val postRecallReleaseDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "LED - date on which offender licence expires.")
  @JsonProperty("licenceExpiryDate")
  val licenceExpiryDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "HDCED - date on which offender will be eligible for home detention curfew.",
  )
  @JsonProperty("homeDetentionCurfewEligibilityDate")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "PED - date on which offender is eligible for parole.",
  )
  @JsonProperty("paroleEligibilityDate")
  val paroleEligibilityDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "HDCAD - the offender's actual home detention curfew date.",
  )
  @JsonProperty("homeDetentionCurfewActualDate")
  val homeDetentionCurfewActualDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "APD - the offender's actual parole date.")
  @JsonProperty("actualParoleDate")
  val actualParoleDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "ROTL - the date on which offender will be released on temporary licence.",
  )
  @JsonProperty("releaseOnTemporaryLicenceDate")
  val releaseOnTemporaryLicenceDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "ERSED - the date on which offender will be eligible for early removal (under the Early Removal Scheme for foreign nationals).",
  )
  @JsonProperty("earlyRemovalSchemeEligibilityDate")
  val earlyRemovalSchemeEligibilityDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "ETD - early term date for offender.")
  @JsonProperty("earlyTermDate")
  val earlyTermDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "MTD - mid term date for offender.")
  @JsonProperty("midTermDate")
  val midTermDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "LTD - late term date for offender.")
  @JsonProperty("lateTermDate")
  val lateTermDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "TUSED - top-up supervision expiry date for offender.",
  )
  @JsonProperty("topupSupervisionExpiryDate")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "Date on which minimum term is reached for parole (indeterminate/life sentences).",
  )
  @JsonProperty("tariffDate")
  val tariffDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "DPRRD - Detention training order post recall release date",
  )
  @JsonProperty("dtoPostRecallReleaseDate")
  val dtoPostRecallReleaseDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "TERSED - Tariff early removal scheme eligibility date",
  )
  @JsonProperty("tariffEarlyRemovalSchemeEligibilityDate")
  val tariffEarlyRemovalSchemeEligibilityDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "Effective sentence end date")
  @JsonProperty("effectiveSentenceEndDate")
  val effectiveSentenceEndDate: LocalDate? = null,

  @Schema(example = "5", description = "ADA - days added to sentence term due to adjustments.")
  @JsonProperty("additionalDaysAwarded")
  val additionalDaysAwarded: Int? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "ARD (override) - automatic (unconditional) release override date for offender.",
  )
  @JsonProperty("automaticReleaseOverrideDate")
  val automaticReleaseOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "CRD (override) - conditional release override date for offender.",
  )
  @JsonProperty("conditionalReleaseOverrideDate")
  val conditionalReleaseOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "NPD (override) - non-parole override date for offender.",
  )
  @JsonProperty("nonParoleOverrideDate")
  val nonParoleOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Wed Apr 01 01:00:00 BST 2020",
    description = "PRRD (override) - post-recall release override date for offender.",
  )
  @JsonProperty("postRecallReleaseOverrideDate")
  val postRecallReleaseOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Wed Apr 01 01:00:00 BST 2020",
    description = "DPRRD (override) - detention training order post-recall release override date for offender",
  )
  @JsonProperty("dtoPostRecallReleaseDateOverride")
  val dtoPostRecallReleaseDateOverride: LocalDate? = null,

  @Valid
  @Schema(
    example = "Wed Apr 01 01:00:00 BST 2020",
    description = "Release date for non-DTO sentence (if applicable). This will be based on one of ARD, CRD, NPD or PRRD.",
  )
  @JsonProperty("nonDtoReleaseDate")
  val nonDtoReleaseDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "SED (calculated) - date on which sentence expires. (as calculated by NOMIS)",
  )
  @JsonProperty("sentenceExpiryCalculatedDate")
  val sentenceExpiryCalculatedDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Feb 03 00:00:00 GMT 2020", description = "SED (override) - date on which sentence expires.")
  @JsonProperty("sentenceExpiryOverrideDate")
  val sentenceExpiryOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "LED (calculated) - date on which offender licence expires. (as calculated by NOMIS)",
  )
  @JsonProperty("licenceExpiryCalculatedDate")
  val licenceExpiryCalculatedDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "LED (override) - date on which offender licence expires.",
  )
  @JsonProperty("licenceExpiryOverrideDate")
  val licenceExpiryOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "PED (calculated) - date on which offender is eligible for parole.",
  )
  @JsonProperty("paroleEligibilityCalculatedDate")
  val paroleEligibilityCalculatedDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "PED (override) - date on which offender is eligible for parole.",
  )
  @JsonProperty("paroleEligibilityOverrideDate")
  val paroleEligibilityOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "TUSED (calculated) - top-up supervision expiry date for offender.",
  )
  @JsonProperty("topupSupervisionExpiryCalculatedDate")
  val topupSupervisionExpiryCalculatedDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "TUSED (override) - top-up supervision expiry date for offender.",
  )
  @JsonProperty("topupSupervisionExpiryOverrideDate")
  val topupSupervisionExpiryOverrideDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "HDCED (calculated) - date on which offender will be eligible for home detention curfew.",
  )
  @JsonProperty("homeDetentionCurfewEligibilityCalculatedDate")
  val homeDetentionCurfewEligibilityCalculatedDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Feb 03 00:00:00 GMT 2020",
    description = "HDCED (override) - date on which offender will be eligible for home detention curfew.",
  )
  @JsonProperty("homeDetentionCurfewEligibilityOverrideDate")
  val homeDetentionCurfewEligibilityOverrideDate: LocalDate? = null,

  @Valid
  @Schema(example = "Mon Apr 20 01:00:00 BST 2020", description = "Confirmed release date for offender.")
  @JsonProperty("confirmedReleaseDate")
  val confirmedReleaseDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Wed Apr 01 01:00:00 BST 2020",
    description = "Confirmed, actual, approved, provisional or calculated release date for offender, according to offender release date algorithm.<h3>Algorithm</h3><ul><li>If there is a confirmed release date, the offender release date is the confirmed release date.</li><li>If there is no confirmed release date for the offender, the offender release date is either the actual parole date or the home detention curfew actual date.</li><li>If there is no confirmed release date, actual parole date or home detention curfew actual date for the offender, the release date is the later of the nonDtoReleaseDate or midTermDate value (if either or both are present)</li></ul>",
  )
  @JsonProperty("releaseDate")
  val releaseDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Apr 01 01:00:00 BST 2019",
    description = "Top-up supervision start date for offender - calculated as licence end date + 1 day or releaseDate if licence end date not set.",
  )
  @JsonProperty("topupSupervisionStartDate")
  val topupSupervisionStartDate: LocalDate? = null,

  @Valid
  @Schema(
    example = "Mon Apr 01 01:00:00 BST 2019",
    description = "Offender's home detention curfew end date - calculated as one day before the releaseDate.",
  )
  @JsonProperty("homeDetentionCurfewEndDate")
  val homeDetentionCurfewEndDate: LocalDate? = null,
) {

  /**
   * Indicates which type of non-DTO release date is the effective release date. One of 'ARD', 'CRD', 'NPD' or 'PRRD'.
   * Values: aRD,cRD,nPD,pRRD
   */
  enum class NonDtoReleaseDateType(val value: String) {

    @JsonProperty("ARD")
    ARD("ARD"),

    @JsonProperty("CRD")
    CRD("CRD"),

    @JsonProperty("NPD")
    NPD("NPD"),

    @JsonProperty("PRRD")
    PRRD("PRRD"),
  }
}
