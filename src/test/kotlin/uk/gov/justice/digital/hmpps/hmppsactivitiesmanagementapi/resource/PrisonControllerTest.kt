package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalDateTime

@WebMvcTest(controllers = [PrisonController::class])
@ContextConfiguration(classes = [PrisonController::class])
class PrisonControllerTest : ControllerTestBase<PrisonController>() {

  @MockBean
  private lateinit var activityService: ActivityService

  @MockBean
  private lateinit var scheduleService: ActivityScheduleService

  @MockBean
  private lateinit var prisonRegimeService: PrisonRegimeService

  override fun controller() = PrisonController(activityService, scheduleService, prisonRegimeService)

  @Test
  fun `200 response when get category activities`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
        prisonCode = MOORLAND_PRISON_CODE,
        attendanceRequired = true,
        inCell = false,
        onWing = false,
        offWing = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        summary = "activity summary",
        description = "activity description",
        riskLevel = "High",
        category = ActivityCategory(
          id = 1L,
          code = "LEISURE_SOCIAL",
          name = "Leisure and social",
          description = "Such as association, library time and social clubs, like music or art",
        ),
        capacity = 20,
        allocated = 10,
        createdTime = LocalDateTime.now(),
        activityState = ActivityState.LIVE,
        paid = true,
      ),
    )

    whenever(activityService.getActivitiesByCategoryInPrison(MOORLAND_PRISON_CODE, 1)).thenReturn(
      expectedModel,
    )

    val response = mockMvc.getActivitiesInCategory(MOORLAND_PRISON_CODE, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService, times(1)).getActivitiesByCategoryInPrison(MOORLAND_PRISON_CODE, 1)
  }

  @Test
  fun `404 response when get category activities and category does not exist`() {
    whenever(activityService.getActivitiesByCategoryInPrison(MOORLAND_PRISON_CODE, 2)).thenThrow(
      EntityNotFoundException("not found"),
    )

    val response = mockMvc.getActivitiesInCategory(MOORLAND_PRISON_CODE, 2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }.andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(activityService, times(1)).getActivitiesByCategoryInPrison(MOORLAND_PRISON_CODE, 2)
  }

  private fun MockMvc.getActivitiesInCategory(prisonCode: String, categoryId: Long) =
    get("/prison/{prisonCode}/activity-categories/{categoryId}/activities", prisonCode, categoryId)

  private fun MockMvc.getCategoryCapacity(prisonCode: String, categoryId: Long) =
    get("/prison/{prisonCode}/activity-categories/{categoryId}/capacity", prisonCode, categoryId)

  @Test
  fun `200 response when get activities`() {
    val expectedModel = listOf(
      ActivitySummary(
        id = 1,
        activityName = "Book club",
        category = ActivityCategory(
          id = 1L,
          code = "LEISURE_SOCIAL",
          name = "Leisure and social",
          description = "Such as association, library time and social clubs, like music or art",
        ),
        capacity = 20,
        allocated = 10,
        waitlisted = 3,
        createdTime = LocalDateTime.now(),
        activityState = ActivityState.LIVE,
      ),
    )

    whenever(activityService.getActivitiesInPrison(MOORLAND_PRISON_CODE, true)).thenReturn(
      expectedModel,
    )

    val response = mockMvc.getActivities(MOORLAND_PRISON_CODE)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService, times(1)).getActivitiesInPrison(MOORLAND_PRISON_CODE, true)
  }

  private fun MockMvc.getActivities(prisonCode: String) =
    get("/prison/{prisonCode}/activities", prisonCode)

  @Test
  fun `200 response when get pay bands by Moorland prison code`() {
    val prisonPayBands = prisonPayBandsLowMediumHigh().map { it.toModelPrisonPayBand() }

    whenever(prisonRegimeService.getPayBandsForPrison(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBands)

    val response = mockMvc.getPrisonPayBandsBy(MOORLAND_PRISON_CODE)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(prisonPayBands))

    verify(prisonRegimeService).getPayBandsForPrison(MOORLAND_PRISON_CODE)
  }

  @Test
  fun `200 response when get prison by code found`() {
    val prisonRegime = transform(prisonRegime(), DayOfWeek.MONDAY)

    whenever(prisonRegimeService.getPrisonRegimeByPrisonCode(PENTONVILLE_PRISON_CODE)).thenReturn(listOf(prisonRegime))

    val response = mockMvc.getPrisonRegimeByPrisonCode(PENTONVILLE_PRISON_CODE)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(prisonRegime)))

    verify(prisonRegimeService).getPrisonRegimeByPrisonCode(PENTONVILLE_PRISON_CODE)
  }

  private fun MockMvc.getPrisonPayBandsBy(prisonCode: String) =
    get("/prison/$prisonCode/prison-pay-bands")

  private fun MockMvc.getPrisonRegimeByPrisonCode(prisonCode: String) =
    get("/prison/prison-regime/$prisonCode")
}
