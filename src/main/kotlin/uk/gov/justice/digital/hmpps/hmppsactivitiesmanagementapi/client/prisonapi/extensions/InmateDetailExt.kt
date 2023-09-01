package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import java.time.LocalDate

fun InmateDetail.isReleasedOnDeath(): Boolean = this.legalStatus == InmateDetail.LegalStatus.DEAD

fun InmateDetail.isReleasedFromRemand(): Boolean = isInactiveOut() && sentenceDetail?.confirmedReleaseDate == null

fun InmateDetail.isReleasedFromCustodialSentence(): Boolean =
  isInactiveOut() && sentenceDetail?.confirmedReleaseDate?.onOrBefore(LocalDate.now()) == true

fun InmateDetail.isInactiveOut(): Boolean = status == "INACTIVE OUT"

fun InmateDetail.isActiveInPrison(prisonCode: String): Boolean = status == "ACTIVE IN" && agencyId == prisonCode

fun InmateDetail.isActiveOutPrison(prisonCode: String): Boolean = status == "ACTIVE OUT" && agencyId == prisonCode
