package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * User Details
 * @param staffId Staff Id
 * @param username Username
 * @param firstName First Name
 * @param lastName Last Name
 * @param accountStatus Status of the User Account
 * @param lockDate Date the user account was locked
 * @param active Indicate if the account is active
 * @param thumbnailId Image Thumbnail Id
 * @param activeCaseLoadId Current Active Caseload
 * @param expiryDate Date the user account has expired
 * @param lockedFlag The User account is locked
 * @param expiredFlag Indicates the user account has expired
 */
data class UserDetail(
  @Schema(example = "231232", required = true, description = "Staff Id")
  @get:JsonProperty("staffId", required = true) val staffId: Long,

  @Schema(example = "DEMO_USER1", required = true, description = "Username")
  @get:JsonProperty("username", required = true) val username: String,

  @Schema(example = "John", required = true, description = "First Name")
  @get:JsonProperty("firstName", required = true) val firstName: String,

  @Schema(example = "Smith", required = true, description = "Last Name")
  @get:JsonProperty("lastName", required = true) val lastName: String,

  @Schema(example = "ACTIVE", required = true, description = "Status of the User Account")
  @get:JsonProperty("accountStatus", required = true) val accountStatus: AccountStatus,

  @Schema(example = "2021-07-05T10:35:17", required = true, description = "Date the user account was locked")
  @get:JsonProperty("lockDate", required = true) val lockDate: String,

  @Schema(example = "true", required = true, description = "Indicate if the account is active")
  @get:JsonProperty("active", required = true) val active: Boolean,

  @Schema(example = "2342341224", description = "Image Thumbnail Id")
  @get:JsonProperty("thumbnailId") val thumbnailId: Long? = null,

  @Schema(example = "MDI", description = "Current Active Caseload")
  @get:JsonProperty("activeCaseLoadId") val activeCaseLoadId: String? = null,

  @Schema(example = "2021-07-05T10:35:17", description = "Date the user account has expired")
  @get:JsonProperty("expiryDate") val expiryDate: String? = null,

  @Schema(example = "false", description = "The User account is locked")
  @get:JsonProperty("lockedFlag") val lockedFlag: Boolean? = null,

  @Schema(example = "true", description = "Indicates the user account has expired")
  @get:JsonProperty("expiredFlag") val expiredFlag: Boolean? = null
) {

  /**
   * Status of the User Account
   * Values: ACTIVE,INACT,SUS,CAREER,MAT,SAB,SICK
   */
  enum class AccountStatus(val value: String) {
    @JsonProperty("ACTIVE") ACTIVE("ACTIVE"),
    @JsonProperty("INACT") INACT("INACT"),
    @JsonProperty("SUS") SUS("SUS"),
    @JsonProperty("CAREER") CAREER("CAREER"),
    @JsonProperty("MAT") MAT("MAT"),
    @JsonProperty("SAB") SAB("SAB"),
    @JsonProperty("SICK") SICK("SICK")
  }
}

