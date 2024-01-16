package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.weeksAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CandidatesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import java.security.Principal
import java.time.LocalDate

@WebMvcTest(controllers = [ActivityScheduleController::class])
@ContextConfiguration(classes = [ActivityScheduleController::class])
class ActivityScheduleControllerTest : ControllerTestBase<ActivityScheduleController>() {

  @MockBean
  private lateinit var activityScheduleService: ActivityScheduleService

  @MockBean
  private lateinit var candidatesService: CandidatesService

  @MockBean
  private lateinit var waitingListService: WaitingListService

  override fun controller() = ActivityScheduleController(activityScheduleService, candidatesService, waitingListService)

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
  fun `200 response when get schedule by schedule identifier with earliest session date default`() {
    val expected = activityEntity().schedules().first().copy(1).toModelSchedule()

    whenever(activityScheduleService.getScheduleById(1, 4.weeksAgo())) doReturn expected

    val response = mockMvc.getScheduleById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    response.contentAsString isEqualTo mapper.writeValueAsString(expected)
  }

  @Test
  fun `200 response when get schedule by schedule identifier with earliest session date specified`() {
    val expected = activityEntity().schedules().first().copy(1).toModelSchedule()

    whenever(activityScheduleService.getScheduleById(1, 4.weeksAgo())) doReturn expected

    val response = mockMvc.getScheduleById(1, 4.weeksAgo())
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    response.contentAsString isEqualTo mapper.writeValueAsString(expected)
  }

  @Test
  fun `404 response when get schedule by id not found`() {
    whenever(activityScheduleService.getScheduleById(eq(-99), any())).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getScheduleById(-99)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("not found")
  }

  private fun MockMvc.getScheduleById(scheduleId: Long, earliestSessionDate: LocalDate? = null) =
    get("/schedules/$scheduleId${earliestSessionDate?.let { "?earliestSessionDate=$it" } ?: ""}")

  @Test
  fun `204 response when allocate offender to a schedule`() {
    val request = PrisonerAllocationRequest(
      prisonerNumber = "654321",
      payBandId = 1,
      startDate = TimeSource.tomorrow(),
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
      mockMvc.allocate(1, PrisonerAllocationRequest(prisonerNumber = null))
        .andExpect { status { isBadRequest() } }
        .andReturn().response,
    ) {
      assertThat(contentAsString).contains("Prisoner number must be supplied")
    }

    with(
      mockMvc.allocate(1, PrisonerAllocationRequest(prisonerNumber = "TOOMANYCHARACTERS"))
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
      reasonCode = DeallocationReason.RELEASED.name,
      endDate = TimeSource.tomorrow(),
      caseNote = null,
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

  @Test
  fun `200 response when get waiting lists by schedule identifier`() {
    val waitingList = waitingList().toModel()

    whenever(waitingListService.getWaitingListsBySchedule(1)).thenReturn(listOf(waitingList))

    val response = mockMvc.getWaitingListsScheduleById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(waitingList)))
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests() {
    @Nested
    @DisplayName("Allocate offender tests")
    inner class AllocateOffenderTests() {
      private var prisonerAllocationRequest = PrisonerAllocationRequest(
        prisonerNumber = "A1234CB",
        payBandId = 1,
        startDate = LocalDate.now().plusDays(1),
      )

      @Test
      @WithMockUser(roles = ["ACTIVITY_ADMIN"])
      fun `Allocate offender (ROLE_ACTIVITY_ADMIN) - 204`() {
        mockMvcWithSecurity.post("/schedules/1/allocations") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(prisonerAllocationRequest)
        }.andExpect { status { isNoContent() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Allocate offender (ROLE_ACTIVITY_HUB) - 204`() {
        mockMvcWithSecurity.post("/schedules/1/allocations") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(prisonerAllocationRequest)
        }.andExpect { status { isNoContent() } }
      }

      @Test
      @WithMockUser(roles = ["PRISON"])
      fun `Allocate offender (ROLE_PRISON) - 403`() {
        mockMvcWithSecurity.post("/schedules/1/allocations") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(prisonerAllocationRequest)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Deallocate offender tests")
    inner class DeallocateOffenderTests() {
      private var prisonerDeallocateRequest = PrisonerDeallocationRequest(
        prisonerNumbers = listOf("654321"),
        reasonCode = DeallocationReason.RELEASED.name,
        endDate = TimeSource.tomorrow(),
        caseNote = null,
      )

      @Test
      @WithMockUser(roles = ["ACTIVITY_ADMIN"])
      fun `Deallocate offender (ROLE_ACTIVITY_ADMIN) - 204`() {
        mockMvcWithSecurity.put("/schedules/1/deallocate") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(prisonerDeallocateRequest)
        }.andExpect { status { isNoContent() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Deallocate offender (ROLE_ACTIVITY_HUB) - 204`() {
        mockMvcWithSecurity.put("/schedules/1/deallocate") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(prisonerDeallocateRequest)
        }.andExpect { status { isNoContent() } }
      }

      @Test
      @WithMockUser(roles = ["PRISON"])
      fun `Deallocate offender (ROLE_PRISON) - 403`() {
        mockMvcWithSecurity.put("/schedules/1/deallocate") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(prisonerDeallocateRequest)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Get schedule by id")
    inner class GetScheduleByIdTests() {
      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Get schedule by id (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvcWithSecurity.get("/schedules/1") {
          contentType = MediaType.APPLICATION_JSON
          header(CASELOAD_ID, "MDI")
        }.andExpect { status { isOk() } }
      }
    }
  }

  private fun MockMvc.getWaitingListsScheduleById(scheduleId: Long) =
    get("/schedules/$scheduleId/waiting-list-applications")
}
