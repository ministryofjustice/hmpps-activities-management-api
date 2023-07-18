package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import org.springframework.security.oauth2.jwt.Jwt

class ClientDetails(authToken: Jwt? = null, val caseLoadId: String? = null) {

  val isClientToken = authToken?.claims?.containsKey("client_id") == true

  val hasAdminRole = authToken?.claims?.containsKey("roles") == true && (authToken.claims?.get("roles") as List<*>).contains("ACTIVITY_ADMIN")
}
