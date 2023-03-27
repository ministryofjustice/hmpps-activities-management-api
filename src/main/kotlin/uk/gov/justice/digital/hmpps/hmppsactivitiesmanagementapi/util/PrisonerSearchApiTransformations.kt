package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary

/**
 * Transform functions providing a thin layer to transform prisoner search api types into their API model equivalents and vice-versa.
 */

fun Prisoner.toSummary() =
  PrisonerSummary(
    prisonerNumber,
    bookingId?.toLong() ?: -1,
    firstName,
    lastName,
    prisonId ?: "UNKNOWN",
    cellLocation ?: "UNKNOWN",
  )

fun List<Prisoner>.toSummary() = map { it.toSummary() }
