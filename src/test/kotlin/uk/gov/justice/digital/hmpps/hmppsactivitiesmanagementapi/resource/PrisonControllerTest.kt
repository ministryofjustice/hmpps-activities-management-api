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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import javax.persistence.EntityNotFoundException

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
  private lateinit var activityService: ActivityService

  @MockBean
  private lateinit var scheduleService: ActivityScheduleService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders.standaloneSetup(
      PrisonController(
        capacityService, activityService, scheduleService
      )
    ).setControllerAdvice(ControllerAdvice(mapper)).build()
  }

  @Test
  fun `200 response when get category capacity`() {
    val expectedModel = CapacityAndAllocated(capacity = 200, allocated = 100)

    whenever(capacityService.getActivityCategoryCapacityAndAllocated("MDI", 1)).thenReturn(
      expectedModel
    )

    val response = mockMvc.getCategoryCapacity("MDI", "1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(capacityService, times(1)).getActivityCategoryCapacityAndAllocated("MDI", 1)
  }

  @Test
  fun `404 response when get category capacity and category does not exist`() {
    whenever(capacityService.getActivityCategoryCapacityAndAllocated("MDI", 2)).thenThrow(
      EntityNotFoundException("not found")
    )

    val response = mockMvc.getCategoryCapacity("MDI", "2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }.andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(capacityService, times(1)).getActivityCategoryCapacityAndAllocated("MDI", 2)
  }

  @Test
  fun `200 response when get category activities`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
        prisonCode = "MDI",
        summary = "activity summary",
        description = "activity description"
      )
    )

    whenever(activityService.getActivitiesByCategoryInPrison("MDI", 1)).thenReturn(
      expectedModel
    )

    val response = mockMvc.getActivitiesInCategory("MDI", "1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService, times(1)).getActivitiesByCategoryInPrison("MDI", 1)
  }

  @Test
  fun `404 response when get category activities and category does not exist`() {
    whenever(activityService.getActivitiesByCategoryInPrison("MDI", 2)).thenThrow(
      EntityNotFoundException("not found")
    )

    val response = mockMvc.getActivitiesInCategory("MDI", "2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }.andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(activityService, times(1)).getActivitiesByCategoryInPrison("MDI", 2)
  }

  private fun MockMvc.getActivitiesInCategory(prisonCode: String, categoryId: String) =
    get("/prison/{prisonCode}/activity-categories/{categoryId}/activities", prisonCode, categoryId)

  private fun MockMvc.getCategoryCapacity(prisonCode: String, categoryId: String) =
    get("/prison/{prisonCode}/activity-categories/{categoryId}/capacity", prisonCode, categoryId)
}
