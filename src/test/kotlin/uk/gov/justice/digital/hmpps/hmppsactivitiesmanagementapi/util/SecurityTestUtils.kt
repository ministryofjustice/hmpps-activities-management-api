package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

object SecurityTestUtils {

  fun setLoggedInUser(username: String) {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    val principal = mock<UserDetails>()
    whenever(securityContext.authentication).thenReturn(authentication)
    whenever(authentication.principal).thenReturn(principal)
    whenever(principal.username).thenReturn(username)
    SecurityContextHolder.setContext(securityContext)
  }
}
