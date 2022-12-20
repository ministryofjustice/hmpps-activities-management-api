package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository

@WebMvcTest(controllers = [ActivityCategoryController::class])
@ContextConfiguration(classes = [ActivityCategoryController::class])
class ActivityCategoryControllerTest : ControllerTestBase<ActivityCategoryController>() {

  @MockBean
  private lateinit var activityCategoryRepository: ActivityCategoryRepository

  override fun controller() = ActivityCategoryController(activityCategoryRepository)

  @Test
  fun `200 response when get activity categories`() {
    val expectedModel = listOf(
      ActivityCategory(
        id = 1,
        code = "category code",
        name = "category name",
        description = "category description"
      )
    )

    whenever(activityCategoryRepository.findAll()).thenReturn(listOf(activityCategory()))

    val response = mockMvc
      .get("/activity-categories")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityCategoryRepository, times(1)).findAll()
  }
}
