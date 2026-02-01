package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@WebMvcTest(controllers = [ActivityController::class])
@ContextConfiguration(classes = [ActivityController::class])
class ActivityControllerTest : ControllerTestBase() {

  @MockitoBean
  private lateinit var activityService: ActivityService

  @Test
  fun `createActivity - success`() {
    val createActivityRequest = activityCreateRequest()
    val createActivityResponse = activityModel(activityEntity())

    whenever(activityService.createActivity(createActivityRequest, user.name)).thenReturn(createActivityResponse)

    val response =
      mockMvc.post("/activities") {
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          createActivityRequest,
        )
      }
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isCreated() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(createActivityResponse))

    verify(activityService).createActivity(any(), any())
  }

  @Test
  fun `createActivity - no request body`() {
    mockMvc.post("/activities") {
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
  fun `createActivity - invalid, empty json`() {
    mockMvc.post("/activities") {
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
            value(containsString("Category ID must be supplied"))
            value(containsString("Activity summary must be supplied"))
            value(containsString("Risk level must be supplied"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `createActivity - invalid, required properties missing`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-invalid-1.json")

    mockMvc.post("/activities") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        createActivityRequest,
      )
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(containsString("Tier code must be supplied"))
            value(containsString("Prison code must be supplied"))
            value(containsString("Category ID must be supplied"))
            value(containsString("Activity summary must be supplied"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `createActivity - invalid, rate 0 or negative`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-invalid-2.json")

    mockMvc.post("/activities") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        createActivityRequest,
      )
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(containsString("The piece rate must be a positive integer"))
            value(containsString("The piece rate items must be a positive integer"))
            value(containsString("The earning rate must be a positive integer"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `createActivity - invalid, character lengths exceeded`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-invalid-3.json")

    mockMvc.post("/activities") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        createActivityRequest,
      )
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(containsString("Incentive level should not exceed 50 characters"))
            value(containsString("Summary should not exceed 50 characters"))
            value(containsString("Pay band must be supplied"))
            value(containsString("Prison code should not exceed 3 characters"))
            value(containsString("Risk level should not exceed 10 characters"))
            value(containsString("Description should not exceed 300 characters"))
            value(containsString("Education level code should not exceed 10 characters"))
            value(containsString("Education level description should not exceed 60 characters"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `200 response when get activity schedules`() {
    val expectedModel = listOf(
      ActivityScheduleLite(
        id = 1,
        description = "schedule description",
        internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
        capacity = 20,
        activity = ActivityLite(
          id = 12L,
          prisonCode = "MDI",
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          offWing = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          summary = "Maths",
          description = "Beginner maths",
          riskLevel = "High",
          category = ActivityCategory(
            id = 1L,
            code = "EDUCATION",
            name = "Education",
            description = "Such as classes in English, maths, construction and computer skills",
          ),
          capacity = 20,
          allocated = 10,
          createdTime = LocalDateTime.now(),
          activityState = ActivityState.LIVE,
          paid = true,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            timeSlot = TimeSlot.AM,
            weekNumber = 1,
            startTime = LocalTime.of(10, 20),
            endTime = LocalTime.of(10, 20),
            daysOfWeek = listOf("Mon"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.now(),
        scheduleWeeks = 1,
        usePrisonRegimeTime = true,
      ),
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

  @Test
  fun `updateActivity - success`() {
    val updateActivityRequest = mapper.read<ActivityUpdateRequest>("activity/activity-update-request-2.json")

    val updateActivityResponse: Activity = mapper.read("activity/activity-update-response-1.json")

    whenever(activityService.updateActivity(PENTONVILLE_PRISON_CODE, 17, updateActivityRequest, user.name)).thenReturn(
      updateActivityResponse,
    )

    val response =
      mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/17") {
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          updateActivityRequest,
        )
      }
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isAccepted() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(updateActivityResponse))

    verify(activityService).updateActivity(any(), any(), any(), any(), any())
  }

  @Test
  fun `updateActivity - fails for paid activity with no pay rates`() {
    mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/1") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(ActivityUpdateRequest(paid = true))
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(containsString("Paid activity must have at least one pay rate associated with it"))
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `updateActivity - no request body`() {
    mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/17") {
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
            value("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ActivityController.update(java.lang.String,long,java.security.Principal,uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest)")
          }
        }
      }

    verifyNoInteractions(activityService)
  }

  @Test
  fun `404 response when get activity id not found`() {
    val updateActivityRequest = mapper.read<ActivityUpdateRequest>("activity/activity-update-request-2.json")

    whenever(activityService.updateActivity(PENTONVILLE_PRISON_CODE, 17, updateActivityRequest, user.name)).thenThrow(
      EntityNotFoundException("not found"),
    )

    mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/17") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        updateActivityRequest,
      )
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isNotFound() }
      }

    verify(activityService).updateActivity(any(), any(), any(), any(), any())
  }

  @Test
  @WithMockAuthUser(roles = ["ACTIVITY_HUB"])
  fun `404 not found response while getting activity pay history for an invalid activity id`() {
    whenever(activityService.getActivityPayHistory(1)).thenThrow(
      EntityNotFoundException("not found"),
    )

    mockMvc.get("/activities/1/pay-history") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isNotFound() }
      }

    verify(activityService).getActivityPayHistory(1)
  }

  @Test
  @WithMockAuthUser(roles = ["ACTIVITY_HUB"])
  fun `200 response while getting activity pay history for a valid activity id`() {
    val expectedModel = listOf(
      ActivityPayHistory(
        id = 29,
        incentiveNomisCode = "BAS",
        incentiveLevel = "Basic",
        PrisonPayBand(
          id = 19,
          displaySequence = 3,
          alias = "Pay band 3",
          description = "Pay band 3",
          nomisPayBand = 3,
          prisonCode = "RSI",
          createdTime = null,
          createdBy = null,
          updatedTime = null,
          updatedBy = null,
        ),
        rate = 100,
        startDate = null,
        changedDetails = "New pay rate added: £1.00",
        changedTime = LocalDateTime.now(),
        changedBy = "joebloggs",
      ),
      ActivityPayHistory(
        id = 37,
        incentiveNomisCode = "STD",
        incentiveLevel = "Standard",
        PrisonPayBand(
          id = 19,
          displaySequence = 3,
          alias = "Pay band 4",
          description = "Pay band 4",
          nomisPayBand = 4,
          prisonCode = "RSI",
          createdTime = null,
          createdBy = null,
          updatedTime = null,
          updatedBy = null,
        ),
        rate = 150,
        startDate = null,
        changedDetails = "New pay rate added: £1.50",
        changedTime = LocalDateTime.now(),
        changedBy = "ABC456 - N. John",
      ),
    )
    whenever(activityService.getActivityPayHistory(1)).thenReturn(expectedModel)

    val response = mockMvc.get("/activities/1/pay-history") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isOk() }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService).getActivityPayHistory(1)
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests {
    @Nested
    @DisplayName("Create activity")
    inner class CreateAuthTests {
      @Test
      fun `createActivity (ROLE_ACTIVITY_HUB) - 201`() {
        mockMvc.post("/activities") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityCreateRequest())
        }.andExpect { status { isCreated() } }
      }

      @Test
      fun `createActivity (ROLE_ACTIVITY_ADMIN) - 201`() {
        mockMvc.post("/activities") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityCreateRequest())
        }.andExpect { status { isCreated() } }
      }

      @Test
      @WithMockAuthUser(roles = ["PRISON"])
      fun `createActivity (ROLE_PRISON) - 403`() {
        mockMvc.post("/activities") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityCreateRequest())
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Update activity")
    inner class UpdateAuthTests {
      @Test
      fun `updateActivity (ROLE_ACTIVITY_HUB) - 202`() {
        val updateActivityRequest = mapper.read<ActivityUpdateRequest>("activity/activity-update-request-2.json")

        mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/17") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(updateActivityRequest)
        }.andExpect { status { isAccepted() } }
      }

      @Test
      fun `updateActivity (ROLE_ACTIVITY_ADMIN) - 202`() {
        val updateActivityRequest = mapper.read<ActivityUpdateRequest>("activity/activity-update-request-2.json")

        mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/17") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(updateActivityRequest)
        }.andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockAuthUser(roles = ["PRISON"])
      fun `updateActivity (ROLE_PRISON) - 403`() {
        val updateActivityRequest = mapper.read<ActivityUpdateRequest>("activity/activity-update-request-2.json")

        mockMvc.patch("/activities/$PENTONVILLE_PRISON_CODE/activityId/17") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(updateActivityRequest)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Get filtered activity by id")
    inner class GetFilteredActivityByIdTests {
      @Test
      @WithMockAuthUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Get filtered activity by id (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvc.get("/activities/1/filtered") {
          contentType = MediaType.APPLICATION_JSON
          header(CASELOAD_ID, "MDI")
        }.andExpect { status { isOk() } }
      }
    }

    @Nested
    @DisplayName("Get pay rate history activity by id")
    inner class GetActivityPayHistoryAuthTests {
      @Test
      @WithMockAuthUser(roles = ["PRISON"])
      fun `Get activity pay rate history by id (PRISON) - 200`() {
        mockMvc.get("/activities/1/pay-history") {
          contentType = MediaType.APPLICATION_JSON
          header(CASELOAD_ID, "MDI")
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockAuthUser(roles = ["ACTIVITY_HUB"])
      fun `Get activity pay rate history by id (ACTIVITY_HUB) - 200`() {
        mockMvc.get("/activities/1/pay-history") {
          contentType = MediaType.APPLICATION_JSON
          header(CASELOAD_ID, "MDI")
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockAuthUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Get activity pay rate history by id (NOMIS_ACTIVITIES) - 403`() {
        mockMvc.get("/activities/1/pay-history") {
          contentType = MediaType.APPLICATION_JSON
          header(CASELOAD_ID, "MDI")
        }.andExpect { status { isForbidden() } }
      }
    }
  }

  private fun MockMvc.getActivitySchedules(id: Long) = get("/activities/{activityId}/schedules", id)
}
