package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID

fun checkCaseLoadAccess(prisonCode: String) {
  val httpRequest = if (RequestContextHolder.getRequestAttributes() != null) (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request else null
  val auth = if (SecurityContextHolder.getContext()?.authentication != null) SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken else null
  val caseLoadId = httpRequest?.getHeader(CASELOAD_ID)

  if (tokenIsNotAClientToken(auth) &&
    tokenDoesNotHaveTheActivityAdminRole(auth) &&
    caseLoadIdRequestHeaderDoesNotMatchPrisonCode(caseLoadId, prisonCode)
  ) {
    throw CaseLoadAccessException()
  }
}

private fun tokenIsNotAClientToken(auth: AuthAwareAuthenticationToken?) = auth == null || !auth.isClientToken

private fun tokenDoesNotHaveTheActivityAdminRole(auth: AuthAwareAuthenticationToken?) = auth == null || !auth.roles.contains(ACTIVITY_ADMIN)

private fun caseLoadIdRequestHeaderDoesNotMatchPrisonCode(caseLoadIdRequestHeader: String?, prisonCode: String) = caseLoadIdRequestHeader != prisonCode

class CaseLoadAccessException : RuntimeException()
