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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ModelTest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate

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

    assertThat(response.contentAsString).contains("Not found")

    verify(prisonService).getByPrisonCode("PVX")
  }

  @Test
  fun `get list of all rolled out prisons`() {
    val originalRolloutDate = LocalDate.parse("01 Feb 2023", ModelTest.dateFormatter)
    val rolloutPrison = RolloutPrisonPlan(
      prisonCode = "LPI",
      activitiesRolledOut = true,
      activitiesRolloutDate = originalRolloutDate,
      appointmentsRolledOut = true,
      appointmentsRolloutDate = originalRolloutDate,
    )
    whenever(prisonService.getRolloutPrisons()).thenReturn(listOf(rolloutPrison))

    val response = mockMvc.get("/rollout")
      .andExpect { status { isOk() } }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(rolloutPrison)))
  }
  private fun MockMvc.getPrisonByCode(code: String) = get("/rollout/{code}", code)
}
