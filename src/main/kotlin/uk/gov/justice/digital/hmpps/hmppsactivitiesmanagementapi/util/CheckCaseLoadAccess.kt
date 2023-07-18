package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID

fun checkCaseLoadAccess(prisonCode: String) {
  val httpRequest = if (RequestContextHolder.getRequestAttributes() != null) (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request else null
  val jwt = if (SecurityContextHolder.getContext()?.authentication?.credentials != null) SecurityContextHolder.getContext().authentication.credentials as Jwt else null
  val caseLoadId = httpRequest?.getHeader(CASELOAD_ID)

  if (!isClientToken(jwt) &&
    !hasAdminRole(jwt) &&
    (prisonCode != caseLoadId)
  ) {
    throw CaseLoadAccessException()
  }
}

fun isClientToken(jwt: Jwt?) = jwt?.claims?.containsKey("client_id") == true

fun hasAdminRole(jwt: Jwt?) = jwt?.claims?.containsKey("roles") == true && (jwt.claims?.get("roles") as List<*>).contains("ACTIVITY_ADMIN")

class CaseLoadAccessException : RuntimeException()
