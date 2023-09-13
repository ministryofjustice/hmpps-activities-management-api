package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner as PrisonerSearchApiPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary as ModelPrisonerSummary

/**
 * Transform functions providing a thin layer to transform prisoner search api types into their API model equivalents and vice-versa.
 */

fun PrisonerSearchApiPrisoner.toSummary() =
  ModelPrisonerSummary(
    prisonerNumber,
    bookingId?.toLong() ?: -1,
    firstName,
    lastName,
    prisonId ?: "UNKNOWN",
    cellLocation ?: "UNKNOWN",
  )

fun PrisonerSearchApiPrisoner?.toSummary(prisonNumber: String, bookingId: Long) =
  this?.toSummary() ?: ModelPrisonerSummary(
    prisonNumber,
    bookingId,
    "UNKNOWN",
    "UNKNOWN",
    "UNKNOWN",
    "UNKNOWN",
  )

fun List<PrisonerSearchApiPrisoner>.toSummary() = map { it.toSummary() }
