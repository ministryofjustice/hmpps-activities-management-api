package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN

class CheckCaseLoadAccessTest {

  private val prisonCode = "MDI"

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `should throw exception if no client token and not admin user and wrong caseload id `() {
    addCaseloadIdToRequestHeader("PVI")
    assertThrows<CaseloadAccessException> { checkCaseloadAccess(prisonCode) }
  }

  @Test
  fun `should throw exception if no client token and not admin user and null caseload id `() {
    assertThrows<CaseloadAccessException> { checkCaseloadAccess(prisonCode) }
  }

  @Test
  fun `should not throw exception if client token present`() {
    setTokenExpectations(isUserToken = false)
    assertDoesNotThrow { checkCaseloadAccess(prisonCode) }
  }

  @Test
  fun `should not throw exception if admin user`() {
    setTokenExpectations(hasAdminRole = true)
    assertDoesNotThrow { checkCaseloadAccess(prisonCode) }
  }

  @Test
  fun `should not throw exception if case load id matches prison code`() {
    addCaseloadIdToRequestHeader(prisonCode)
    assertDoesNotThrow { checkCaseloadAccess(prisonCode) }
  }

  private fun setTokenExpectations(isUserToken: Boolean = true, hasAdminRole: Boolean = false) {
    val token = mock<AuthAwareAuthenticationToken>()
    val securityContext = mock<SecurityContext>()
    SecurityContextHolder.setContext(securityContext)

    whenever(securityContext.authentication).thenReturn(token)
    whenever(token.isUserToken).thenReturn(isUserToken)

    if (hasAdminRole) {
      val roles = setOf(SimpleGrantedAuthority(ROLE_ACTIVITY_ADMIN))
      whenever(token.authorities).thenReturn(roles)
    }
  }
}
