package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import java.time.LocalDate

fun Prisoner.isOutOfPrison() = inOutStatus == Prisoner.InOutStatus.OUT

fun Prisoner.lastMovementType(): MovementType? =
  MovementType.entries.firstOrNull { it.nomisShortCode == lastMovementTypeCode }

fun Prisoner.isInactiveOut(): Boolean = status == "INACTIVE OUT"

fun Prisoner.isRestrictedPatient(): Boolean = restrictedPatient == true

fun Prisoner.isActiveAtPrison(prisonCode: String) = prisonId == prisonCode && (isActiveIn() || isActiveOut())

fun Prisoner.isActiveOut(): Boolean = status == "ACTIVE OUT"

fun Prisoner.isActiveIn(): Boolean = status == "ACTIVE IN"

fun Prisoner.isActiveInPrison(prisonCode: String) = prisonId == prisonCode && isActiveIn()

fun Prisoner.isActiveIn(prisonCode: String): Boolean = isActiveIn() && prisonId == prisonCode

fun Prisoner.isTemporarilyReleased() =
  (confirmedReleaseDate == null || confirmedReleaseDate.isAfter(LocalDate.now())) && isActiveOut() && lastMovementType() != MovementType.RELEASE

fun Prisoner.isPermanentlyReleased() =
  isInactiveOut() && confirmedReleaseDate?.onOrBefore(LocalDate.now()) == true && lastMovementType() == MovementType.RELEASE

fun Prisoner.isAtDifferentLocationTo(prisonCode: String) = prisonCode != prisonId

enum class MovementType(val nomisShortCode: String) {
  RELEASE("REL"),
  TEMPORARY_ABSENCE("TAP"),
  TRANSFER("TRN"),
}
