package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class UserSummary(
  @Schema(
    description = "The NOMIS STAFF_MEMBERS.STAFF_ID value for mapping to NOMIS.",
    example = "36",
  )
  val id: Long,

  @Schema(
    description = "The NOMIS STAFF_USER_ACCOUNTS.USERNAME value for mapping to NOMIS",
    example = "AAA01U",
  )
  val username: String,

  @Schema(
    description = "The user's first name",
    example = "Alice",
  )
  val firstName: String,

  @Schema(
    description = "The user's last name",
    example = "Akbar",
  )
  val lastName: String,
)
