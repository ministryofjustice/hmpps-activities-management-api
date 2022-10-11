package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.transform
import javax.persistence.EntityNotFoundException

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [RolloutPrisonController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [RolloutPrisonController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class RolloutPrisonControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var prisonService: RolloutPrisonService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(RolloutPrisonController(prisonService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `200 response when get prison by code found`() {
    val rolloutPrison = transform(rolloutPrison())

    whenever(prisonService.getByPrisonCode("PVI")).thenReturn(rolloutPrison)

    val response = mockMvc.getPrisonByCode("PVI")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(rolloutPrison))

    verify(prisonService).getByPrisonCode("PVI")
  }

  @Test
  fun `404 response when get prison by code not found`() {
    whenever(prisonService.getByPrisonCode("PVX")).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getPrisonByCode("PVX")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(prisonService).getByPrisonCode("PVX")
  }

  private fun MockMvc.getPrisonByCode(code: String) = get("/rolloutPrisons/{code}", code)
}
