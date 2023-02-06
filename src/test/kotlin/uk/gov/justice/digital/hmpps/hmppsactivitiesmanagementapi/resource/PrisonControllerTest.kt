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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate

@WebMvcTest(controllers = [PrisonController::class])
@ContextConfiguration(classes = [PrisonController::class])
class PrisonControllerTest : ControllerTestBase<PrisonController>() {

  @MockBean
  private lateinit var capacityService: CapacityService

  @MockBean
  private lateinit var activityService: ActivityService

  @MockBean
  private lateinit var scheduleService: ActivityScheduleService

  @MockBean
  private lateinit var prisonRegimeService: PrisonRegimeService

  override fun controller() = PrisonController(capacityService, activityService, scheduleService, prisonRegimeService)

  @Test
  fun `200 response when get category capacity`() {
    val expectedModel = CapacityAndAllocated(capacity = 200, allocated = 100)

    whenever(capacityService.getActivityCategoryCapacityAndAllocated(moorlandPrisonCode, 1)).thenReturn(
      expectedModel
    )

    val response = mockMvc.getCategoryCapacity(moorlandPrisonCode, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(capacityService, times(1)).getActivityCategoryCapacityAndAllocated(moorlandPrisonCode, 1)
  }

  @Test
  fun `404 response when get category capacity and category does not exist`() {
    whenever(capacityService.getActivityCategoryCapacityAndAllocated(moorlandPrisonCode, 2)).thenThrow(
      EntityNotFoundException("not found")
    )

    val response = mockMvc.getCategoryCapacity(moorlandPrisonCode, 2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }.andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(capacityService, times(1)).getActivityCategoryCapacityAndAllocated(moorlandPrisonCode, 2)
  }

  @Test
  fun `200 response when get category activities`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
        prisonCode = moorlandPrisonCode,
        attendanceRequired = true,
        inCell = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        summary = "activity summary",
        description = "activity description",
        riskLevel = "High",
        minimumIncentiveLevel = "Basic",
        category = ActivityCategory(
          id = 1L,
          code = "LEISURE_SOCIAL",
          name = "Leisure and social",
          description = "Such as association, library time and social clubs, like music or art"
        )
      )
    )

    whenever(activityService.getActivitiesByCategoryInPrison(moorlandPrisonCode, 1)).thenReturn(
      expectedModel
    )

    val response = mockMvc.getActivitiesInCategory(moorlandPrisonCode, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService, times(1)).getActivitiesByCategoryInPrison(moorlandPrisonCode, 1)
  }

  @Test
  fun `404 response when get category activities and category does not exist`() {
    whenever(activityService.getActivitiesByCategoryInPrison(moorlandPrisonCode, 2)).thenThrow(
      EntityNotFoundException("not found")
    )

    val response = mockMvc.getActivitiesInCategory(moorlandPrisonCode, 2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }.andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(activityService, times(1)).getActivitiesByCategoryInPrison(moorlandPrisonCode, 2)
  }

  private fun MockMvc.getActivitiesInCategory(prisonCode: String, categoryId: Long) =
    get("/prison/{prisonCode}/activity-categories/{categoryId}/activities", prisonCode, categoryId)

  private fun MockMvc.getCategoryCapacity(prisonCode: String, categoryId: Long) =
    get("/prison/{prisonCode}/activity-categories/{categoryId}/capacity", prisonCode, categoryId)

  @Test
  fun `200 response when get activities`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
        prisonCode = moorlandPrisonCode,
        attendanceRequired = true,
        inCell = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        summary = "activity summary",
        description = "activity description",
        riskLevel = "High",
        minimumIncentiveLevel = "Basic",
        category = ActivityCategory(
          id = 1L,
          code = "LEISURE_SOCIAL",
          name = "Leisure and social",
          description = "Such as association, library time and social clubs, like music or art"
        )
      )
    )

    whenever(activityService.getActivitiesInPrison(moorlandPrisonCode)).thenReturn(
      expectedModel
    )

    val response = mockMvc.getActivities(moorlandPrisonCode)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(activityService, times(1)).getActivitiesInPrison(moorlandPrisonCode)
  }

  private fun MockMvc.getActivities(prisonCode: String) =
    get("/prison/{prisonCode}/activities", prisonCode)

  @Test
  fun `200 response when get schedule by prison code and search criteria found`() {
    val schedules = activityModel(activityEntity()).schedules

    whenever(
      scheduleService.getActivitySchedulesByPrisonCode(
        pentonvillePrisonCode,
        LocalDate.MIN,
        TimeSlot.AM,
        1
      )
    ).thenReturn(
      schedules
    )

    val response = mockMvc.getSchedulesBy(pentonvillePrisonCode, LocalDate.MIN, TimeSlot.AM, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(schedules))

    verify(scheduleService).getActivitySchedulesByPrisonCode(
      pentonvillePrisonCode,
      LocalDate.MIN,
      TimeSlot.AM,
      1
    )
  }

  @Test
  fun `200 response when get schedule by prison code and search criteria not found`() {
    whenever(
      scheduleService.getActivitySchedulesByPrisonCode(
        pentonvillePrisonCode,
        LocalDate.MIN,
        TimeSlot.AM,
        1
      )
    ).thenReturn(
      emptyList()
    )

    val response = mockMvc.getSchedulesBy(pentonvillePrisonCode, LocalDate.MIN, TimeSlot.AM, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("[]")

    verify(scheduleService).getActivitySchedulesByPrisonCode(
      pentonvillePrisonCode,
      LocalDate.MIN,
      TimeSlot.AM,
      1
    )
  }

  private fun MockMvc.getSchedulesBy(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot,
    locationId: Long
  ) =
    get("/prison/$prisonCode/schedules?date=$date&timeSlot=$timeSlot&locationId=$locationId")

  @Test
  fun `200 response when get pay bands by Moorland prison code`() {
    val prisonPayBands = prisonPayBandsLowMediumHigh().map { it.toModelPrisonPayBand() }

    whenever(prisonRegimeService.getPayBandsForPrison(moorlandPrisonCode)).thenReturn(prisonPayBands)

    val response = mockMvc.getPrisonPayBandsBy(moorlandPrisonCode)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(prisonPayBands))

    verify(prisonRegimeService).getPayBandsForPrison(moorlandPrisonCode)
  }

  @Test
  fun `200 response when get prison by code found`() {
    val prisonRegime = transform(prisonRegime())

    whenever(prisonRegimeService.getPrisonRegimeByPrisonCode(pentonvillePrisonCode)).thenReturn(prisonRegime)

    val response = mockMvc.getPrisonRegimeByPrisonCode(pentonvillePrisonCode)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(prisonRegime))

    verify(prisonRegimeService).getPrisonRegimeByPrisonCode(pentonvillePrisonCode)
  }

  @Test
  fun `404 response when get prison by code not found`() {
    whenever(prisonRegimeService.getPrisonRegimeByPrisonCode("PVX")).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getPrisonRegimeByPrisonCode("PVX")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(prisonRegimeService).getPrisonRegimeByPrisonCode("PVX")
  }

  private fun MockMvc.getPrisonPayBandsBy(prisonCode: String) =
    get("/prison/$prisonCode/prison-pay-bands")

  private fun MockMvc.getPrisonRegimeByPrisonCode(prisonCode: String) =
    get("/prison/prison-regime/$prisonCode")
}
