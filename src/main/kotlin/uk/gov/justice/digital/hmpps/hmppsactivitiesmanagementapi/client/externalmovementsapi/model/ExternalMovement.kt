package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

data class ExternalMovement(
  val id: UUID,

  @JsonProperty("personIdentifier")
  val prisonerNumber: String,

  val description: ExternalMovementDescription,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val status: ExternalMovementStatus,
  val detail: ExternalMovementDetail,
  val isSensitive: Boolean,
)

data class ExternalMovementDescription(
  val full: String,
  val short: String,
  val code: String,
)

data class ExternalMovementStatus(
  val code: String,
  val description: String,
  val hintText: String? = null,
)

data class ExternalMovementDetail(
  val uiUrl: String,
  val requiredRoles: Set<String>,
)
