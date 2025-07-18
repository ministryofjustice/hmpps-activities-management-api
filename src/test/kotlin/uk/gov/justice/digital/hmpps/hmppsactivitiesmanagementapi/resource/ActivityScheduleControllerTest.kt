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
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.weeksAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.earliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.CandidatesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [ActivityScheduleController::class])
@ContextConfiguration(classes = [ActivityScheduleController::class])
class ActivityScheduleControllerTest : ControllerTestBase<ActivityScheduleController>() {

  @MockitoBean
  private lateinit var activityScheduleService: ActivityScheduleService

  @MockitoBean
  private lateinit var candidatesService: CandidatesService

  @MockitoBean
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

  private fun nonAssociations() = listOf(
    NonAssociationDetails(
      allocated = true,
      reasonCode = "BULLYING",
      reasonDescription = "Bullying",
      roleCode = "VICTIM",
      roleDescription = "Victim",
      restrictionType = "LANDING",
      restrictionTypeDescription = "Landing",
      otherPrisonerDetails = OtherPrisonerDetails(
        prisonerNumber = "ZZ3333Z",
        firstName = "Joe",
        lastName = "Bloggs",
        cellLocation = "F-2-009",
      ),
      whenUpdated = LocalDateTime.now(),
      comments = "Bullying comment",
    ),
  )

  @Test
  fun `200 response when get non-associations is successful`() {
    val expected = nonAssociations()

    whenever(candidatesService.nonAssociations(123, "A1234DD")) doReturn expected

    val response = mockMvc.getNonAssociations(123, "A1234DD")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    response.contentAsString isEqualTo mapper.writeValueAsString(expected)
  }

  private fun MockMvc.getAllocationsByScheduleId(scheduleId: Long) = get("/schedules/$scheduleId/allocations")

  private fun MockMvc.getNonAssociations(scheduleId: Long, prisonerNumber: String) = get("/schedules/$scheduleId/non-associations?prisonerNumber=$prisonerNumber")

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
    whenever(activityScheduleService.getScheduleById(eq(-99), any(), eq(false))).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getScheduleById(-99)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("not found")
  }

  private fun MockMvc.getScheduleById(scheduleId: Long, earliestSessionDate: LocalDate? = null) = get("/schedules/$scheduleId${earliestSessionDate?.let { "?earliestSessionDate=$it" } ?: ""}")

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

    verify(activityScheduleService, never()).allocatePrisoner(any(), any(), any(), any())
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

  private fun MockMvc.allocate(scheduleId: Long, request: PrisonerAllocationRequest) = post("/schedules/$scheduleId/allocations") {
    principal = Principal { "USERNAME" }
    content = mapper.writeValueAsString(request)
    contentType = MediaType.APPLICATION_JSON
  }

  private fun MockMvc.deallocate(scheduleId: Long, request: PrisonerDeallocationRequest) = put("/schedules/$scheduleId/deallocate") {
    principal = Principal { "USERNAME" }
    content = mapper.writeValueAsString(request)
    contentType = MediaType.APPLICATION_JSON
  }

  @Test
  fun `200 response when get waiting lists by schedule identifier with includeNonAssociations = false`() {
    val earliestReleaseDate = earliestReleaseDate()
    val waitingList = waitingList().toModel(earliestReleaseDate)

    whenever(waitingListService.getWaitingListsBySchedule(1, false)).thenReturn(listOf(waitingList))

    val response = mockMvc.getWaitingListsScheduleById(1, false)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(waitingList)))

    verify(waitingListService).getWaitingListsBySchedule(1, false)
  }

  @Test
  fun `200 response when get waiting lists by schedule identifier with includeNonAssociations = true`() {
    val earliestReleaseDate = earliestReleaseDate()
    val waitingList = waitingList().toModel(earliestReleaseDate)

    whenever(waitingListService.getWaitingListsBySchedule(1, true)).thenReturn(listOf(waitingList))

    val response = mockMvc.getWaitingListsScheduleById(1, true)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(waitingList)))

    verify(waitingListService).getWaitingListsBySchedule(1, true)
  }

  @Test
  fun `200 response when get waiting lists by schedule identifier with includeNonAssociations = null`() {
    val earliestReleaseDate = earliestReleaseDate()
    val waitingList = waitingList().toModel(earliestReleaseDate)

    whenever(waitingListService.getWaitingListsBySchedule(1, true)).thenReturn(listOf(waitingList))

    val response = mockMvc.getWaitingListsScheduleById(1, null)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(waitingList)))

    verify(waitingListService).getWaitingListsBySchedule(1, true)
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests {
    @Nested
    @DisplayName("Allocate offender tests")
    inner class AllocateOffenderTests {
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
    inner class DeallocateOffenderTests {
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
    inner class GetScheduleByIdTests {
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

  private fun MockMvc.getWaitingListsScheduleById(scheduleId: Long, includeNonAssociationsCheck: Boolean? = null) = get("/schedules/$scheduleId/waiting-list-applications") {
    if (includeNonAssociationsCheck != null) param("includeNonAssociationsCheck", includeNonAssociationsCheck.toString())
  }
}
