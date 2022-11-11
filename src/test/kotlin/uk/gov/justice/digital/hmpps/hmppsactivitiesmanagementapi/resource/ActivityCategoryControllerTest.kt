package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.times
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [ActivityCategoryController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ActivityCategoryController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class ActivityCategoryControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var activityCategoryRepository: ActivityCategoryRepository

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(ActivityCategoryController(activityCategoryRepository))
      .setControllerAdvice(ControllerAdvice(mapper))
      .build()
  }

  @Test
  fun `200 response when get activity categories`() {
    val expectedModel = listOf(
      ActivityCategory(
        id = 1,
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
