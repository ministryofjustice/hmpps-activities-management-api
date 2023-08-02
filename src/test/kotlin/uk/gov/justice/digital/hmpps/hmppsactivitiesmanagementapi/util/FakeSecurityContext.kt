package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.AuthAwareAuthenticationToken

const val DEFAULT_USERNAME = "Bob"

class FakeSecurityContext(val username: String = DEFAULT_USERNAME) : BeforeEachCallback, AfterEachCallback {

  private val authentication: AuthAwareAuthenticationToken = mock {
    on { principal } doReturn username
    on { name } doReturn username
    on { isUserToken } doReturn true
  }
  private val securityContext: SecurityContext = mock { on { authentication } doReturn authentication }

  override fun beforeEach(context: ExtensionContext?) {
    SecurityContextHolder.setContext(securityContext)
  }

  override fun afterEach(context: ExtensionContext?) {
    SecurityContextHolder.clearContext()
  }
}
