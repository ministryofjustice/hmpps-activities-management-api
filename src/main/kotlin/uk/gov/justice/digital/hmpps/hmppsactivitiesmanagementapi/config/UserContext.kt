package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
object UserContext {
  private val authToken = ThreadLocal<String>()
  private val authentication = ThreadLocal<Authentication>()
  fun setAuthToken(token: String?) = authToken.set(token)
  // fun getAuthToken(): String = authToken.get() - not used yet
  fun setAuthentication(auth: Authentication?) = authentication.set(auth)
  fun getAuthentication(): Authentication = authentication.get()
}
