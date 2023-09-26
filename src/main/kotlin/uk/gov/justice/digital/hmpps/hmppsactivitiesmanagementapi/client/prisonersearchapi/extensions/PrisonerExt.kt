package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import java.time.LocalDate

fun Prisoner.isOutOfPrison() = inOutStatus == Prisoner.InOutStatus.OUT

fun Prisoner.lastMovementType(): MovementType? =
  MovementType.entries.firstOrNull { it.nomisShortCode == lastMovementTypeCode }

fun Prisoner.isInactiveOut(): Boolean = status == "INACTIVE OUT"

fun Prisoner.isActiveOut(): Boolean = status == "ACTIVE OUT"

fun Prisoner.isActiveIn(): Boolean = status == "ACTIVE IN"

fun Prisoner.isTemporarilyReleased() =
  (confirmedReleaseDate == null || confirmedReleaseDate.isAfter(LocalDate.now())) && isActiveOut() && lastMovementType() != MovementType.RELEASE

fun Prisoner.isPermanentlyReleased() =
  isInactiveOut() && confirmedReleaseDate?.onOrBefore(LocalDate.now()) == true && lastMovementType() == MovementType.RELEASE

enum class MovementType(val nomisShortCode: String) {
  RELEASE("REL"),
  TEMPORARY_ABSENCE("TAP"),
  TRANSFER("TRN"),
}
