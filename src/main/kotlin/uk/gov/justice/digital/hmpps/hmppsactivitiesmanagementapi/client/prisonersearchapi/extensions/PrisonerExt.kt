package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner

fun Prisoner.isOut() = inOutStatus == Prisoner.InOutStatus.OUT

fun Prisoner.lastMovementType(): MovementType? =
  MovementType.entries.firstOrNull { it.nomisShortCode == lastMovementTypeCode }

fun Prisoner.isInactiveOut(): Boolean = status == "INACTIVE OUT"

fun Prisoner.isActiveOut(prisonCode: String): Boolean = status == "ACTIVE OUT" && prisonId == prisonCode

enum class MovementType(val nomisShortCode: String) {
  RELEASE("REL"),
  TEMPORARY_ABSENCE("TAP"),
  TRANSFER("TRN"),
}
