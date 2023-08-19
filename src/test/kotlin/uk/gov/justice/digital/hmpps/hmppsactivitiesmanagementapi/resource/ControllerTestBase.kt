package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ResourceServerConfiguration
import java.security.Principal

@ExtendWith(SpringExtension::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ResourceServerConfiguration::class)
@ActiveProfiles("test")
@WebAppConfiguration
abstract class ControllerTestBase<C> {

  val user: Principal = mock { on { name } doReturn "USER" }

  lateinit var mockMvc: MockMvc

  @Autowired
  lateinit var mockMvcWithSecurity: MockMvc

  @Autowired
  lateinit var mapper: ObjectMapper

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(controller())
      .setControllerAdvice(ControllerAdvice(mapper))
      .build()
  }

  internal abstract fun controller(): C

  fun createAuthentication(role: String = "ROLE_PRISON"): Authentication {
    val auth = TestingAuthenticationToken("USER", "password", listOf(SimpleGrantedAuthority(role)))
    SecurityContextHolder.getContext().authentication = auth
    return auth
  }
}
