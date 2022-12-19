package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import java.security.Principal
import java.time.LocalTime
import javax.persistence.EntityNotFoundException

@WebMvcTest(controllers = [ActivityController::class])
@ContextConfiguration(classes = [ActivityController::class])
class ActivityControllerTest : ControllerTestBase<ActivityController>() {

  @MockBean
  private lateinit var activityService: ActivityService

  @MockBean
  private lateinit var capacityService: CapacityService

  override fun controller() = ActivityController(activityService, capacityService)

  @Test
  fun `createActivity - success`() {

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-1.json"),
      object : TypeReference<ActivityCreateRequest>() {}
    )

    val createActivityResponse: Activity = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-response-1.json"),
      object : TypeReference<Activity>() {}
    )

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("USER")

    whenever(activityService.createActivity(any(), any())).thenReturn(createActivityResponse)

    val response =
      mockMvc.post("/activities") {
        principal = mockPrincipal
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          createActivityRequest
        )
      }
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(createActivityResponse))

    verify(activityService).createActivity(any(), any())
  }

  @Test
  fun `createActivity - no request body`() {

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("USER")

    mockMvc.post("/activities") {
      principal = mockPrincipal
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = null
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ActivityController.create(java.security.Principal,uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest)")
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `createActivity - invalid, empty jason`() {

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("USER")

    mockMvc.post("/activities") {
      principal = mockPrincipal
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = "{}"
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(containsString("Prison code must be supplied"))
            value(containsString("Tier ID must be supplied"))
            value(containsString("Activity description must be supplied"))
            value(containsString("Category ID must be supplied"))
            value(containsString("Activity summary must be supplied"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `createActivity - invalid, required properties missing`() {

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-invalid-1.json"),
      object : TypeReference<ActivityCreateRequest>() {}
    )

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("USER")

    mockMvc.post("/activities") {
      principal = mockPrincipal
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        createActivityRequest
      )
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(containsString("Prison code must be supplied"))
            value(containsString("Tier ID must be supplied"))
            value(containsString("Activity description must be supplied"))
            value(containsString("Category ID must be supplied"))
            value(containsString("Activity summary must be supplied"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `200 response when get activity by ID found`() {
    val activity = activityModel(activityEntity())

    whenever(activityService.getActivityById(1)).thenReturn(activity)

    val response = mockMvc.getActivityById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(activity))

    verify(activityService).getActivityById(1)
  }

  @Test
  fun `404 response when get activity by ID not found`() {
    whenever(activityService.getActivityById(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getActivityById(2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(activityService).getActivityById(2)
  }

  @Test
  fun `200 response when get activity capacity`() {
    val expectedModel = CapacityAndAllocated(capacity = 200, allocated = 100)

    whenever(capacityService.getActivityCapacityAndAllocated(1)).thenReturn(expectedModel)

    val response = mockMvc.getActivityCapacity(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(capacityService).getActivityCapacityAndAllocated(1)
  }

  @Test
  fun `404 response when get activity capacity and activity id not found`() {
    whenever(capacityService.getActivityCapacityAndAllocated(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getActivityCapacity(2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(capacityService).getActivityCapacityAndAllocated(2)
  }

  @Test
  fun `200 response when get activity schedules`() {
    val expectedModel = listOf(
      ActivityScheduleLite(
        id = 1,
        description = "schedule description",
        startTime = LocalTime.of(10, 20),
        endTime = LocalTime.of(10, 20),
        internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
        daysOfWeek = listOf("Mon"),
        capacity = 20,
        activity = ActivityLite(
          id = 12L,
          prisonCode = "MDI",
          attendanceRequired = true,
          summary = "Maths",
          description = "Beginner maths",
          category = ActivityCategory(
            id = 1L,
            code = "EDU",
            description = "Education"
          )
        )
      )
    )

    whenever(activityService.getSchedulesForActivity(1)).thenReturn(expectedModel)

    val response = mockMvc.getActivitySchedules(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService).getSchedulesForActivity(1)
  }

  @Test
  fun `404 response when get activity schedules and activity id not found`() {
    whenever(activityService.getSchedulesForActivity(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getActivitySchedules(2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(activityService).getSchedulesForActivity(2)
  }

  private fun MockMvc.getActivityById(id: Long) = get("/activities/{activityId}", id)
  private fun MockMvc.getActivityCapacity(id: Long) = get("/activities/{activityId}/capacity", id)
  private fun MockMvc.getActivitySchedules(id: Long) =
    get("/activities/{activityId}/schedules", id)
}
