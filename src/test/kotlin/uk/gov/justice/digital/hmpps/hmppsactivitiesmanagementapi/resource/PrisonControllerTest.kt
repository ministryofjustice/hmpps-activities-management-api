package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonPayBandCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonPayBandUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
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
  private lateinit var prisonRegimeService: PrisonRegimeService

  override fun controller() = PrisonController(activityService, prisonRegimeService)

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

  @Test
  fun `201 response when create pay band with Moorland prison code`() {
    val prisonPayBands = prisonPayBandsLowMediumHigh().map { it.toModelPrisonPayBand() }

    val request = PrisonPayBandCreateRequest(
      displaySequence = prisonPayBands.first().displaySequence,
      nomisPayBand = prisonPayBands.first().nomisPayBand,
      alias = prisonPayBands.first().alias,
      description = prisonPayBands.first().alias,
    )

    whenever(prisonRegimeService.createPrisonPayBand(eq(MOORLAND_PRISON_CODE), eq(request), eq(user), any())).thenReturn(prisonPayBands.first())

    val response = mockMvc.createPayBand(MOORLAND_PRISON_CODE, request)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(prisonPayBands.first()))

    verify(prisonRegimeService).createPrisonPayBand(eq(MOORLAND_PRISON_CODE), eq(request), eq(user), any())
  }

  @Test
  fun `200 response when update pay band with Moorland prison code`() {
    val prisonPayBands = prisonPayBandsLowMediumHigh().map { it.toModelPrisonPayBand() }

    val request = PrisonPayBandUpdateRequest(
      displaySequence = prisonPayBands.first().displaySequence,
      nomisPayBand = prisonPayBands.first().nomisPayBand,
      alias = prisonPayBands.first().alias,
      description = prisonPayBands.first().alias,
    )

    whenever(prisonRegimeService.updatePrisonPayBand(eq(MOORLAND_PRISON_CODE), eq(1), eq(request), eq(user), any())).thenReturn(prisonPayBands.first())

    val response = mockMvc.updatePayBand(MOORLAND_PRISON_CODE, 1, request)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(prisonPayBands.first()))

    verify(prisonRegimeService).updatePrisonPayBand(eq(MOORLAND_PRISON_CODE), eq(1), eq(request), eq(user), any())
  }

  @Test
  @WithMockUser(roles = ["ACTIVITY_HUB"])
  fun `Create payband (ROLE_ACTIVITY_HUB) - 403`() {
    val request = PrisonPayBandCreateRequest(
      displaySequence = 1,
      nomisPayBand = 1,
      alias = "test",
      description = "test",
    )
    mockMvcWithSecurity.post("/prison/MDI/prison-pay-band") {
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }.andExpect { status { isForbidden() } }
  }

  @Test
  @WithMockUser(roles = ["ACTIVITY_HUB"])
  fun `Update payband (ROLE_ACTIVITY_HUB) - 403`() {
    val request = PrisonPayBandUpdateRequest(
      displaySequence = 1,
      nomisPayBand = 1,
      alias = "test",
      description = "test",
    )
    mockMvcWithSecurity.patch("/prison/MDI/prison-pay-band/1") {
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }.andExpect { status { isForbidden() } }
  }

  private fun MockMvc.getPrisonPayBandsBy(prisonCode: String) =
    get("/prison/$prisonCode/prison-pay-bands")

  private fun MockMvc.createPayBand(prisonCode: String, request: PrisonPayBandCreateRequest) =
    post("/prison/$prisonCode/prison-pay-band") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }

  private fun MockMvc.updatePayBand(prisonCode: String, prisonPayBandId: Int, request: PrisonPayBandUpdateRequest) =
    patch("/prison/$prisonCode/prison-pay-band/$prisonPayBandId") {
      this.principal = user
      contentType = MediaType.APPLICATION_JSON
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }

  private fun MockMvc.getPrisonRegimeByPrisonCode(prisonCode: String) =
    get("/prison/prison-regime/$prisonCode")
}
