package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner

fun Prisoner.isOut() = inOutStatus == Prisoner.InOutStatus.OUT

fun Prisoner.lastMovementType(): MovementType? =
  MovementType.values().firstOrNull { it.nomisShortCode == lastMovementTypeCode }

enum class MovementType(val nomisShortCode: String) {
  RELEASE("REL"),
  TEMPORARY_ABSENCE("TAP"),
  TRANSFER("TRN"),
}
