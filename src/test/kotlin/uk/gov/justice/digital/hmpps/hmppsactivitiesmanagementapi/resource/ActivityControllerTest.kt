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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import javax.persistence.EntityNotFoundException

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [ActivityController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ActivityController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class ActivityControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var activityService: ActivityService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(ActivityController(activityService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `200 response when get activity by ID found`() {
    val activity = activityModel(activityEntity())

    whenever(activityService.getActivityById(1)).thenReturn(activity)

    val response = mockMvc.getActivityById("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(activity))

    verify(activityService).getActivityById(1)
  }

  @Test
  fun `404 response when get activity by ID not found`() {
    whenever(activityService.getActivityById(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getActivityById("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(activityService).getActivityById(2)
  }

  private fun MockMvc.getActivityById(id: String) = get("/activities/{activityId}", id)
}
