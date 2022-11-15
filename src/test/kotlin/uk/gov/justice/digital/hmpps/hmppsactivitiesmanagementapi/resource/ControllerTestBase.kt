package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice

@ExtendWith(SpringExtension::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WebAppConfiguration
abstract class ControllerTestBase<C> {

  lateinit var mockMvc: MockMvc

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
}
