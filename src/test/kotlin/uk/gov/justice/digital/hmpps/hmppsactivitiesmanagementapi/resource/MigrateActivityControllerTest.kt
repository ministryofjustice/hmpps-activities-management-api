package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [MigrateActivityController::class])
@ContextConfiguration(classes = [MigrateActivityController::class])
class MigrateActivityControllerTest : ControllerTestBase<MigrateActivityController>() {

  @MockBean
  private lateinit var activityService: ActivityService

  override fun controller() = MigrateActivityController(activityService)

  @Test
  fun `200 response when migrating an activity`() {
    val expectedResponse = ActivityMigrateResponse(moorlandPrisonCode, 1L, 2L)
    whenever(activityService.migrateActivity(request)).thenReturn(expectedResponse)

    val response = mockMvc.post("/migrate/activity") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))

    verify(activityService).migrateActivity(request)
  }

  // TODO: No suitable role forbidden/access denied

  // TODO: Prison is not rolled out for activities - do not allow migration of an activity

  // TODO: Invalid request - no pay rates

  // TODO: Invalid request - no startDate

  // TODO: Invalid request - invalid category

  // TODO: Invalid request - no schedules

  // TODO: Invalid request - no allocations

  companion object {
    private val startTimeAm = LocalTime.of(9, 23)
    private val startTimePm = LocalTime.of(14, 23)
    private val endTimeAm = LocalTime.of(11, 23)
    private val endTimePm = LocalTime.of(16, 23)
    private val startDate = LocalDate.of(2023, 10, 1)
    private val endDate = LocalDate.of(2024, 10, 1)

    val request = ActivityMigrateRequest(
      programServiceCode = "TEST",
      prisonCode = moorlandPrisonCode,
      startDate,
      endDate,
      minimumIncentiveLevel = "BAS",
      description = "Test activity",
      internalLocationId = 123L,
      internalLocationCode = "WOW",
      internalLocationDescription = "MDI-1-2-WOW",
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
      allocations = listOf(
        NomisAllocation(prisonerNumber = "G4765GG", bookingId = 1, nomisPayBand = "1", startDate, endDate),
      ),
    )
  }
}
