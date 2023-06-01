package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CandidatesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CapacityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import java.security.Principal

@WebMvcTest(controllers = [ActivityScheduleController::class])
@ContextConfiguration(classes = [ActivityScheduleController::class])
class ActivityScheduleControllerTest : ControllerTestBase<ActivityScheduleController>() {

  @MockBean
  private lateinit var activityScheduleService: ActivityScheduleService

  @MockBean
  private lateinit var candidatesService: CandidatesService

  @MockBean
  private lateinit var capacityService: CapacityService

  override fun controller() = ActivityScheduleController(activityScheduleService, candidatesService, capacityService)

  @Test
  fun `200 response when get schedule capacity`() {
    val expectedModel = CapacityAndAllocated(capacity = 200, allocated = 100)

    whenever(capacityService.getActivityScheduleCapacityAndAllocated(1)).thenReturn(expectedModel)

    val response = mockMvc.getActivityScheduleCapacity(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(capacityService).getActivityScheduleCapacityAndAllocated(1)
  }

  @Test
  fun `404 response when get schedule capacity and activity id not found`() {
    whenever(capacityService.getActivityScheduleCapacityAndAllocated(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getActivityScheduleCapacity(2)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(capacityService).getActivityScheduleCapacityAndAllocated(2)
  }

  private fun MockMvc.getActivityScheduleCapacity(activityScheduleId: Long) =
    get("/schedules/{activityScheduleId}/capacity", activityScheduleId)

  @Test
  fun `200 response when get allocations by schedule identifier`() {
    val expectedAllocations = activityEntity().schedules().first().allocations().toModelAllocations()

    whenever(activityScheduleService.getAllocationsBy(1)).thenReturn(expectedAllocations)

    val response = mockMvc.getAllocationsByScheduleId(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedAllocations))

    verify(activityScheduleService).getAllocationsBy(1)
  }

  @Test
  fun `404 response when get allocations by schedule identifier not found`() {
    whenever(activityScheduleService.getAllocationsBy(-99)).thenThrow(EntityNotFoundException("Not found"))

    val response = mockMvc.getAllocationsByScheduleId(-99)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")
  }

  private fun MockMvc.getAllocationsByScheduleId(scheduleId: Long) =
    get("/schedules/$scheduleId/allocations")

  @Test
  fun `200 response when get schedule lite by schedule identifier`() {
    val expected = activityEntity().schedules().first().toModelSchedule()

    whenever(activityScheduleService.getScheduleById(1)).thenReturn(expected)

    val response = mockMvc.getScheduleById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expected))
    verify(activityScheduleService).getScheduleById(1)
  }

  @Test
  fun `404 response when get schedule by id not found`() {
    whenever(activityScheduleService.getScheduleById(-99)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getScheduleById(-99)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("not found")

    verify(activityScheduleService).getScheduleById(-99)
  }

  private fun MockMvc.getScheduleById(scheduleId: Long) =
    get("/schedules/$scheduleId")

  @Test
  fun `204 response when allocate offender to a schedule`() {
    val request = PrisonerAllocationRequest(
      prisonerNumber = "654321",
      payBandId = 1,
    )

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("THE USER NAME")

    mockMvc.allocate(1, request)
      .andExpect { status { isNoContent() } }

    verify(activityScheduleService).allocatePrisoner(1, request, "USERNAME")
  }

  @Test
  fun `400 response when allocate offender to a schedule request constraints are violated`() {
    with(
      mockMvc.allocate(1, PrisonerAllocationRequest(prisonerNumber = null, payBandId = null))
        .andExpect { status { isBadRequest() } }
        .andReturn().response,
    ) {
      assertThat(contentAsString).contains("Prisoner number must be supplied")
      assertThat(contentAsString).contains("Pay band must be supplied")
    }

    with(
      mockMvc.allocate(1, PrisonerAllocationRequest(prisonerNumber = "TOOMANYCHARACTERS", payBandId = 1))
        .andExpect { status { isBadRequest() } }
        .andReturn().response,
    ) {
      assertThat(contentAsString).contains("Prisoner number cannot be more than 7 characters")
    }

    verify(activityScheduleService, never()).allocatePrisoner(any(), any(), any())
  }

  @Test
  fun `204 response when deallocate offender from a schedule`() {
    val request = PrisonerDeallocationRequest(
      prisonerNumbers = listOf("654321"),
      reasonCode = DeallocationReason.RELEASED,
      endDate = TimeSource.tomorrow(),
    )

    val mockPrincipal: Principal = mock()
    whenever(mockPrincipal.name).thenReturn("THE USER NAME")

    mockMvc.deallocate(1, request)
      .andExpect { status { isNoContent() } }

    verify(activityScheduleService).deallocatePrisoners(1, request, "USERNAME")
  }

  private fun MockMvc.allocate(scheduleId: Long, request: PrisonerAllocationRequest) =
    post("/schedules/$scheduleId/allocations") {
      principal = Principal { "USERNAME" }
      content = mapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
    }

  private fun MockMvc.deallocate(scheduleId: Long, request: PrisonerDeallocationRequest) =
    put("/schedules/$scheduleId/deallocate") {
      principal = Principal { "USERNAME" }
      content = mapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
    }
}
