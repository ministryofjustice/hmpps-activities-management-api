package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ClientDetails

fun checkCaseLoadAccess(prisonCode: String, client: ClientDetails) {
  if (!client.isClientToken &&
    !client.hasAdminRole &&
    (prisonCode != client.caseLoadId)
  ) {
    throw CaseLoadAccessException()
  }
}

class CaseLoadAccessException : RuntimeException()
