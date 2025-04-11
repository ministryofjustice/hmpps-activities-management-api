package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MigrateActivityService
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [MigrateActivityController::class])
@ContextConfiguration(classes = [MigrateActivityController::class])
class MigrateActivityControllerTest : ControllerTestBase<MigrateActivityController>() {

  @MockitoBean
  private lateinit var migrateActivityService: MigrateActivityService

  override fun controller() = MigrateActivityController(migrateActivityService)

  @BeforeEach
  fun resetMocks() {
    reset(migrateActivityService)
  }

  @Test
  fun `200 response when migrating an activity`() {
    val expectedResponse = ActivityMigrateResponse(MOORLAND_PRISON_CODE, 1L, 2L)
    whenever(migrateActivityService.migrateActivity(activityRequest)).thenReturn(expectedResponse)

    val response = mockMvc.post("/migrate/activity") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(activityRequest)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))

    verify(migrateActivityService).migrateActivity(activityRequest)
  }

  @Test
  fun `Simulate 400 response for activity migration - could be for a variety of reasons`() {
    whenever(migrateActivityService.migrateActivity(activityRequest))
      .thenThrow(ValidationException("Generic validation exception"))

    mockMvc.post("/migrate/activity") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(activityRequest)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Generic validation exception"))
          }
        }
      }

    verify(migrateActivityService).migrateActivity(activityRequest)
  }

  @Test
  fun `200 response when migrating an allocation`() {
    val expectedResponse = AllocationMigrateResponse(1L, 2L)
    whenever(migrateActivityService.migrateAllocation(allocationRequest)).thenReturn(expectedResponse)

    val response = mockMvc.post("/migrate/allocation") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(allocationRequest)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))

    verify(migrateActivityService).migrateAllocation(allocationRequest)
  }

  @Test
  fun `Move activity start dates - 400 - Missing start date`() {
    mockMvc.post("/migrate/PVI/move-activity-start-dates") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(allocationRequest)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Required request parameter 'activityStartDate' for method parameter type LocalDate is not present"))
          }
        }
      }

    verifyNoInteractions(migrateActivityService)
  }

  @Test
  fun `Move activity start dates - 200`() {
    val tomorrow = LocalDate.now().plusDays(1)

    val expectedResponse = emptyList<String>()
    whenever(migrateActivityService.moveActivityStartDates("PVI", tomorrow)).thenReturn(expectedResponse)

    val response = mockMvc.post("/migrate/PVI/move-activity-start-dates?activityStartDate=$tomorrow") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(allocationRequest)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))

    verify(migrateActivityService).moveActivityStartDates("PVI", tomorrow)
  }

  @Test
  fun `Simulate 400 response for allocation migration - could be for a variety of reasons`() {
    whenever(migrateActivityService.migrateAllocation(allocationRequest))
      .thenThrow(ValidationException("Generic validation exception"))

    mockMvc.post("/migrate/allocation") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(allocationRequest)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Generic validation exception"))
          }
        }
      }

    verify(migrateActivityService).migrateAllocation(allocationRequest)
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests {
    @Nested
    @DisplayName("Migrate allocations")
    inner class MigrateAllocationsTests {
      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Migrate allocation (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvcWithSecurity.post("/migrate/allocation") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Migrate allocation (ROLE_NOMIS_APPOINTMENTS) - 403`() {
        mockMvcWithSecurity.post("/migrate/allocation") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Migrate allocation (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.post("/migrate/allocation") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Migrate allocation (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.post("/migrate/allocation") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Migrate activity")
    inner class MigrateActivityTests {
      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Migrate activity (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvcWithSecurity.post("/migrate/activity") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Migrate activity (ROLE_NOMIS_APPOINTMENTS) - 403`() {
        mockMvcWithSecurity.post("/migrate/allocation") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Migrate activity (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.post("/migrate/activity") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Migrate activity (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.post("/migrate/activity") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Delete activity")
    inner class DeleteActivityTests {
      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Delete activity (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvcWithSecurity.delete("/migrate/delete-activity/prison/MDI/id/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Delete activity (ROLE_NOMIS_APPOINTMENTS) - 403`() {
        mockMvcWithSecurity.delete("/migrate/delete-activity/prison/MDI/id/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Delete activity (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.delete("/migrate/delete-activity/prison/MDI/id/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Delete activity (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.delete("/migrate/delete-activity/prison/MDI/id/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Move Activity Start Dates")
    inner class MoveActivityStartDateTests {
      val tomorrow = LocalDate.now().plusDays(1)

      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Move activity start dates (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvcWithSecurity.post("/migrate/MDI/move-activity-start-dates?activityStartDate=$tomorrow") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_ADMIN"])
      fun `Move activity start dates (ROLE_ACTIVITY_ADMIN) - 200`() {
        mockMvcWithSecurity.post("/migrate/MDI/move-activity-start-dates?activityStartDate=$tomorrow") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Move activity start dates (ROLE_NOMIS_APPOINTMENTS) - 403`() {
        mockMvcWithSecurity.post("/migrate/MDI/move-activity-start-dates?activityStartDate=$tomorrow") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(allocationRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Move activity start dates (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.post("/migrate/MDI/move-activity-start-dates?activityStartDate=$tomorrow") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Move activity start dates (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.post("/migrate/MDI/move-activity-start-dates?activityStartDate=$tomorrow") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(activityRequest)
        }.andExpect { status { isForbidden() } }
      }
    }
  }

  companion object {
    private val startTimeAm = LocalTime.of(9, 23)
    private val startTimePm = LocalTime.of(14, 23)
    private val endTimeAm = LocalTime.of(11, 23)
    private val endTimePm = LocalTime.of(16, 23)
    private val startDate = LocalDate.of(2023, 10, 1)
    private val endDate = LocalDate.of(2024, 10, 1)

    val activityRequest = ActivityMigrateRequest(
      programServiceCode = "TEST",
      prisonCode = MOORLAND_PRISON_CODE,
      startDate,
      endDate,
      description = "Test activity",
      capacity = 10,
      payPerSession = "H",
      runsOnBankHoliday = false,
      payRates = listOf(
        NomisPayRate(nomisPayBand = "1", incentiveLevel = "BAS", rate = 110),
        NomisPayRate(nomisPayBand = "2", incentiveLevel = "BAS", rate = 120),
      ),
      scheduleRules = listOf(
        NomisScheduleRule(startTime = startTimeAm, endTime = endTimeAm, monday = true),
        NomisScheduleRule(startTime = startTimePm, endTime = endTimePm, monday = true),
      ),
    )

    val allocationRequest = AllocationMigrateRequest(
      prisonCode = MOORLAND_PRISON_CODE,
      activityId = 1L,
      splitRegimeActivityId = 2L,
      prisonerNumber = "A1234AA",
      bookingId = 12L,
      cellLocation = "RSI-A-1-2-011",
      nomisPayBand = "1",
      startDate,
      suspendedFlag = false,
    )
  }
}
