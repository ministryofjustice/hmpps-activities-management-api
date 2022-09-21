package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import javax.persistence.EntityNotFoundException

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityControllerTest(
  @Autowired private val wac: WebApplicationContext,
  @Autowired private val mapper: ObjectMapper
) {

  private val mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()

  @MockBean
  private lateinit var activityService: ActivityService

  @Test
  fun `200 response when get activity by ID found`() {
    val activity = activityModel(activityEntity())

    whenever(activityService.getActivityById(1)).thenReturn(activity)

    val response = mockMvc.getActivityById("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(activity))
  }

  @Test
  fun `404 response when get activity by ID not found`() {
    whenever(activityService.getActivityById(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getActivityById("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")
  }

  private fun MockMvc.getActivityById(id: String) = get("/activities/{activityId}", id)
}
