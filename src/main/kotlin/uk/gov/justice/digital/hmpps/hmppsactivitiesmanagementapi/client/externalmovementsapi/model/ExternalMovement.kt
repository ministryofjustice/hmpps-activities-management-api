package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ExternalMovement(
  val id: String,

  @JsonProperty("personIdentifier")
  val prisonerNumber: String,

  val description: ExternalMovementDescription,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val status: ExternalMovementStatus,
  val detail: ExternalMovementDetail? = null,
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
  val uiUrl: String? = null,
  val requiredRoles: List<String>? = null,
)
