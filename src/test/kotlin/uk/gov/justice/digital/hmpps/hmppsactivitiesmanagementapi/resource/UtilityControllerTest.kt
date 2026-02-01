package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.UpdateCaseNoteUUIDResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository.ActivityScheduleWithInvalidLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MigrateCaseNotesUUIDService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.STATUS_COMPLETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.util.*

@WebMvcTest(controllers = [UtilityController::class])
@ContextConfiguration(classes = [UtilityController::class])
class UtilityControllerTest : ControllerTestBase() {

  @MockitoBean
  private lateinit var outboundEventsService: OutboundEventsService

  @MockitoBean
  private lateinit var activityLocationService: ActivityLocationService

  @MockitoBean
  private lateinit var migrateCaseNotesUUIDService: MigrateCaseNotesUUIDService

  private val identifierCaptor = argumentCaptor<Long>()

  @Test
  fun `201 response when outbound event is published`() {
    val response = mockMvc.publishEvents(OutboundEvent.PRISONER_ALLOCATION_AMENDED, listOf(1, 1, 2))
      .andExpect { status { isCreated() } }.andReturn().response

    verify(outboundEventsService, times(2)).send(eq(OutboundEvent.PRISONER_ALLOCATION_AMENDED), identifierCaptor.capture(), eq(null))

    response.contentAsString isEqualTo "Domain event PRISONER_ALLOCATION_AMENDED published"
    identifierCaptor.firstValue isEqualTo 1
    identifierCaptor.secondValue isEqualTo 2
  }

  @Test
  fun `returns a list of activities with invalid location`() {
    val act1 = TestData("RSI", 111, "Activity 1", 333, "LOC-1", "Location 1", UUID.fromString("11111111-1111-1111-1111-111111111111"))
    val act2 = TestData("LEI", 222, "Activity 2", 444, "LOC-2", "Location 2", UUID.fromString("22222222-2222-2222-2222-222222222222"))
    whenever(activityLocationService.getInvalidActivityLocations()).thenReturn(listOf(act1, act2))

    val expectedResult = """
      Prison Code,Activity ID,Activity Description,Internal Location ID,Internal Location Code,Internal Location Description,DPS Location ID
      RSI,111,Activity 1,333,LOC-1,Location 1,11111111-1111-1111-1111-111111111111
      LEI,222,Activity 2,444,LOC-2,Location 2,22222222-2222-2222-2222-222222222222
      
    """.trimIndent()

    val response = mockMvc.getInvalidActivityLocations()
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(expectedResult)
  }

  @Test
  fun `200 response while updating case notes UUID`() {
    val expectedResponse = UpdateCaseNoteUUIDResponse(
      STATUS_COMPLETED,
      STATUS_COMPLETED,
      STATUS_COMPLETED,
      STATUS_COMPLETED,
      STATUS_COMPLETED,
    )
    whenever(migrateCaseNotesUUIDService.updateCaseNoteUUID()).thenReturn(expectedResponse)

    val response = mockMvc.post("/utility/update-case-note-uuid")
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))

    verify(migrateCaseNotesUUIDService).updateCaseNoteUUID()
  }

  private fun MockMvc.publishEvents(event: OutboundEvent, identifiers: List<Long>) = post("/utility/publish-events") {
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      PublishEventUtilityModel(
        outboundEvent = event,
        identifiers = identifiers,
      ),
    )
  }

  private fun MockMvc.getInvalidActivityLocations() = get("/utility/invalid-activity-locations") {}

  inner class TestData(
    private val prisonCode: String,
    private val activityId: Long,
    private val activityDescription: String,
    private val internalLocationId: Int,
    private val internalLocationCode: String,
    private val internalLocationDescription: String,
    private val dpsLocationId: UUID,
  ) : ActivityScheduleWithInvalidLocation {
    override fun getPrisonCode() = this.prisonCode
    override fun getActivityId() = this.activityId
    override fun getActivityDescription() = this.activityDescription
    override fun getInternalLocationId() = this.internalLocationId
    override fun getInternalLocationCode() = this.internalLocationCode
    override fun getInternalLocationDescription() = this.internalLocationDescription
    override fun getDpsLocationId() = this.dpsLocationId
  }
}
