package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate

fun determineEarliestReleaseDate(prisoner: Prisoner): EarliestReleaseDate {
  val releaseDate = when {
    prisoner.releaseDate != null -> prisoner.releaseDate
    hasActualParoleDate(prisoner) -> prisoner.actualParoleDate
    hasTariffDate(prisoner) -> prisoner.tariffDate
    else -> null
  }

  return EarliestReleaseDate(
    releaseDate = when {
      prisoner.releaseDate != null -> prisoner.releaseDate
      hasActualParoleDate(prisoner) -> prisoner.actualParoleDate
      hasTariffDate(prisoner) -> prisoner.tariffDate
      else -> null
    },
    isTariffDate = prisoner.tariffDate?.isEqual(releaseDate) ?: false,
    isConvictedUnsentenced = prisoner.legalStatus == Prisoner.LegalStatus.CONVICTED_UNSENTENCED,
    isImmigrationDetainee = prisoner.legalStatus == Prisoner.LegalStatus.IMMIGRATION_DETAINEE,
    isRemand = prisoner.legalStatus == Prisoner.LegalStatus.REMAND,
    isIndeterminateSentence = prisoner.legalStatus == Prisoner.LegalStatus.INDETERMINATE_SENTENCE,
  )
}

private fun hasActualParoleDate(prisoner: Prisoner) =
  prisoner.legalStatus == Prisoner.LegalStatus.INDETERMINATE_SENTENCE && prisoner.actualParoleDate != null

private fun hasTariffDate(prisoner: Prisoner) =
  prisoner.legalStatus == Prisoner.LegalStatus.INDETERMINATE_SENTENCE && prisoner.tariffDate != null
