package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

/**
 * Incentive & Earned Privilege Summary
 * @param bookingId Offender booking identifier.
 * @param iepLevel The current IEP level (e.g. Basic, Standard or Enhanced).
 * @param iepDate Effective date of current IEP level.
 * @param daysSinceReview The number of days since last review.
 * @param iepTime Effective date & time of current IEP level.
 * @param iepDetails All IEP detail entries for the offender (most recent first).
 */
data class PrivilegeSummary(

  @Schema(example = "112321", description = "Offender booking identifier.")
  @JsonProperty("bookingId") val bookingId: Long,

  @Schema(example = "Basic", description = "The current IEP level (e.g. Basic, Standard or Enhanced).")
  @JsonProperty("iepLevel") val iepLevel: IepLevel,

  @Valid
  @Schema(
    example = "Thu Jan 24 00:00:00 GMT 2019",
    required = true,
    description = "Effective date of current IEP level."
  )
  @JsonProperty("iepDate") val iepDate: java.time.LocalDate,

  @Schema(example = "35", description = "The number of days since last review.")
  @JsonProperty("daysSinceReview") val daysSinceReview: Long,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Effective date & time of current IEP level.")
  @JsonProperty("iepTime") val iepTime: String? = null,

  @Valid
  @Schema(example = "null", description = "All IEP detail entries for the offender (most recent first).")
  @JsonProperty("iepDetails") val iepDetails: List<PrivilegeDetail>? = null
) {

  /**
   * The current IEP level (e.g. Basic, Standard or Enhanced).
   * Values: basic,standard,enhanced
   */
  enum class IepLevel(val value: String) {

    @JsonProperty("Basic")
    Basic("Basic"),
    @JsonProperty("Standard")
    Standard("Standard"),
    @JsonProperty("Enhanced")
    Enhanced("Enhanced")
  }
}
