package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform

@WebMvcTest(controllers = [RolloutController::class])
@ContextConfiguration(classes = [RolloutController::class])
class RolloutControllerTest : ControllerTestBase<RolloutController>() {

  @MockBean
  private lateinit var prisonService: RolloutPrisonService

  override fun controller() = RolloutController(prisonService)

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

    assertThat(response.contentAsString).contains("Not Found")

    verify(prisonService).getByPrisonCode("PVX")
  }

  private fun MockMvc.getPrisonByCode(code: String) = get("/rollout/{code}", code)
}
