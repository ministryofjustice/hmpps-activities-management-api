package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ExternalActivitiesPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ExternalActivitiesPrisonService

@WebMvcTest(controllers = [ExternalActivitiesPrisonController::class])
@ContextConfiguration(classes = [ExternalActivitiesPrisonController::class])
class ExternalActivitiesPrisonControllerTest : ControllerTestBase() {

  @MockitoBean
  private lateinit var externalActivitiesPrisonService: ExternalActivitiesPrisonService

  @Test
  fun `200 response when prisons enabled for external activities are found`() {
    val expectedPrisons = listOf(
      ExternalActivitiesPrison(prisonCode = "AGI", prisonName = "Askham Grange"),
      ExternalActivitiesPrison(prisonCode = "KMI", prisonName = "Kirkham"),
    )

    whenever(externalActivitiesPrisonService.getPrisonsEnabledForExternalActivities()).thenReturn(expectedPrisons)

    val response = mockMvc.getExternalActivitiesPrisons()
      .andExpect { status { isOk() } }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedPrisons))

    verify(externalActivitiesPrisonService).getPrisonsEnabledForExternalActivities()
  }

  @Test
  fun `200 response with an empty list when no prisons are enabled for external activities`() {
    whenever(externalActivitiesPrisonService.getPrisonsEnabledForExternalActivities()).thenReturn(emptyList())

    mockMvc.getExternalActivitiesPrisons()
      .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
      .andExpect { status { isOk() } }
      .andExpect { content { json("[]") } }

    verify(externalActivitiesPrisonService).getPrisonsEnabledForExternalActivities()
  }

  @Nested
  @DisplayName("Authorization tests for getting prisons enabled for external activities")
  inner class AuthorizationTestsForExternalActivities {
    @Test
    @WithMockUser(roles = ["ACTIVITY_ADMIN"])
    fun `200 response when user role is valid`() {
      whenever(externalActivitiesPrisonService.getPrisonsEnabledForExternalActivities()).thenReturn(emptyList())

      mockMvc.getExternalActivitiesPrisons()
        .andExpect { status { isOk() } }
        .andExpect { content { json("[]") } }

      verify(externalActivitiesPrisonService).getPrisonsEnabledForExternalActivities()
    }

    @Test
    @WithMockUser(roles = ["INVALID_ROLE"])
    fun `403 response when user role is invalid`() {
      mockMvc.getExternalActivitiesPrisons()
        .andExpect { status { isForbidden() } }

      verifyNoInteractions(externalActivitiesPrisonService)
    }

    @Test
    @WithAnonymousUser
    fun `should return 401 when the user is unauthorised`() {
      mockMvc.getExternalActivitiesPrisons()
        .andExpect { status { isUnauthorized() } }

      verifyNoInteractions(externalActivitiesPrisonService)
    }
  }

  private fun MockMvc.getExternalActivitiesPrisons() = get("/external-activities/prisons")
}
