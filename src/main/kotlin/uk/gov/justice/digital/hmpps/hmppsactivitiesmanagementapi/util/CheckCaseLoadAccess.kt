package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

fun checkCaseLoadAccess(prisonCode: String, caseLoadId: String?) {
  if (prisonCode != caseLoadId) throw CaseLoadAccessException()
}

class CaseLoadAccessException : RuntimeException()
