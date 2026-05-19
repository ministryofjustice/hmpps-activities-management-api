package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun externalMovement(
  id: UUID = UUID.randomUUID(),
  prisonerNumber: String = "A11111A",
  fullDescription: String = "Standard ROTL",
  shortDescription: String = "Accommodation-related",
  code: String = "FB",
  start: LocalDateTime = LocalDate.now().atTime(9, 0),
  end: LocalDateTime = LocalDate.now().atTime(17, 0),
  statusCode: String = "SCHEDULED",
  statusDescription: String = "Scheduled",
  uiUrl: String = "TestUrl",
  requiredRoles: Set<String> = setOf("TEST_ROLE"),
  isSensitive: Boolean = false,
) = ExternalMovement(
  id = id,
  prisonerNumber = prisonerNumber,
  description = ExternalMovementDescription(full = fullDescription, short = shortDescription, code = code),
  start = start,
  end = end,
  status = ExternalMovementStatus(code = statusCode, description = statusDescription),
  detail = ExternalMovementDetail(uiUrl = uiUrl, requiredRoles = requiredRoles),
  isSensitive = isSensitive,
)
