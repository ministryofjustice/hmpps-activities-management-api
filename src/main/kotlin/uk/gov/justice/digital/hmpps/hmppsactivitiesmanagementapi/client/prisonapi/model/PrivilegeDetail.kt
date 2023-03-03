package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

/**
 * Incentive & Earned Privilege Details
 * @param bookingId Offender booking identifier.
 * @param sequence Sequence Number of IEP Level
 * @param iepDate Effective date of IEP level.
 * @param agencyId Identifier of Agency this privilege entry is associated with.
 * @param iepLevel The IEP level (e.g. Basic, Standard or Enhanced).
 * @param iepTime Effective date & time of IEP level.
 * @param comments Further details relating to this privilege entry.
 * @param userId Identifier of user related to this privilege entry.
 * @param auditModuleName The Screen (e.g. NOMIS screen OIDOIEPS) or system (PRISON_API) that made the change
 */
data class PrivilegeDetail(

  @Schema(example = "null", description = "Offender booking identifier.")
  @JsonProperty("bookingId")
  val bookingId: Long,

  @Schema(example = "1", description = "Sequence Number of IEP Level")
  @JsonProperty("sequence")
  val sequence: Long,

  @Valid
  @Schema(example = "null", description = "Effective date of IEP level.")
  @JsonProperty("iepDate")
  val iepDate: java.time.LocalDate,

  @Schema(
    example = "null",
    required = true,
    description = "Identifier of Agency this privilege entry is associated with.",
  )
  @JsonProperty("agencyId")
  val agencyId: String,

  @Schema(example = "null", description = "The IEP level (e.g. Basic, Standard or Enhanced).")
  @JsonProperty("iepLevel")
  val iepLevel: String,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Effective date & time of IEP level.")
  @JsonProperty("iepTime")
  val iepTime: String? = null,

  @Schema(example = "null", description = "Further details relating to this privilege entry.")
  @JsonProperty("comments")
  val comments: String? = null,

  @Schema(example = "null", description = "Identifier of user related to this privilege entry.")
  @JsonProperty("userId")
  val userId: String? = null,

  @Schema(
    example = "PRISON_API",
    description = "The Screen (e.g. NOMIS screen OIDOIEPS) or system (PRISON_API) that made the change",
  )
  @JsonProperty("auditModuleName")
  val auditModuleName: String? = null,
)
