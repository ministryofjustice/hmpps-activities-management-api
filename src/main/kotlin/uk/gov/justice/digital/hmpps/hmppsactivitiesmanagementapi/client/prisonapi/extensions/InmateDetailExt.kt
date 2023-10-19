package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail

fun InmateDetail.isActiveInPrison(prisonCode: String): Boolean = status == "ACTIVE IN" && agencyId == prisonCode

fun InmateDetail.isActiveOutPrison(prisonCode: String): Boolean = status == "ACTIVE OUT" && agencyId == prisonCode
