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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [PrisonController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [PrisonController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class PrisonControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var capacityService: CapacityService

  @MockBean
  private lateinit var scheduleService: ActivityScheduleService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(PrisonController(capacityService, scheduleService))
      .setControllerAdvice(ControllerAdvice(mapper))
      .build()
  }

  @Test
  fun `200 response when get category capacity`() {
    val expectedModel = CapacityAndAllocated(capacity = 200, allocated = 100)

    whenever(capacityService.getActivityCategoryCapacityAndAllocated("MDI", 1)).thenReturn(
      expectedModel
    )

    val response = mockMvc
      .get("/prison/MDI/activity-categories/1/capacity")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(capacityService, times(1)).getActivityCategoryCapacityAndAllocated("MDI", 1)
  }
}
