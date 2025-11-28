package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.earliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerWaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.security.Principal

@WebMvcTest(controllers = [WaitingListApplicationController::class])
@ContextConfiguration(classes = [WaitingListApplicationController::class])
class WaitingListApplicationControllerTest : ControllerTestBase<WaitingListApplicationController>() {

  @MockitoBean
  private lateinit var waitingListService: WaitingListService

  override fun controller(): WaitingListApplicationController = WaitingListApplicationController(waitingListService)

  @Test
  fun `200 response when get by ID found`() {
    val waitingListApplication = waitingList().toModel(earliestReleaseDate())

    whenever(waitingListService.getWaitingListBy(waitingListApplication.id)).thenReturn(waitingListApplication)

    val response = mockMvc.get("/waiting-list-applications/${waitingListApplication.id}")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(waitingListApplication))

    verify(waitingListService).getWaitingListBy(waitingListApplication.id)
  }

  @Test
  fun `202 response when update waiting list`() {
    val waitingListApplication = waitingList().toModel(earliestReleaseDate())

    whenever(waitingListService.updateWaitingList(waitingListApplication.id, WaitingListApplicationUpdateRequest(), user.name)).thenReturn(waitingListApplication)

    val response = mockMvc.updatedWaitingList(waitingListApplication.id, WaitingListApplicationUpdateRequest(), user)
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(waitingListApplication))

    verify(waitingListService).updateWaitingList(waitingListApplication.id, WaitingListApplicationUpdateRequest(), user.name)
  }

  @Test
  fun `200 response when searching waiting list application`() {
    val request = WaitingListSearchRequest()
    val waitingListApplication = waitingList().toModel(earliestReleaseDate())
    val pagedResult = PageImpl(listOf(waitingListApplication), Pageable.ofSize(1), 1)

    whenever(
      waitingListService.searchWaitingLists(
        "MDI",
        request,
        0,
        50,
      ),
    ).thenReturn(pagedResult)

    val response = mockMvc.post("/waiting-list-applications/MDI/search") {
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    JSONAssert.assertEquals((response.contentAsString), mapper.writeValueAsString(pagedResult), false)
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests {
    @Nested
    @DisplayName("Waiting list application")
    inner class WaitingListApplicationTests {
      @Test
      @WithMockUser(roles = ["ACTIVITY_ADMIN"])
      fun `Update waiting list application (ROLE_ACTIVITY_ADMIN) - 202`() {
        mockMvcWithSecurity.patch("/waiting-list-applications/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(WaitingListApplicationUpdateRequest())
        }.andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Update waiting list application (ROLE_ACTIVITY_HUB) - 202`() {
        mockMvcWithSecurity.patch("/waiting-list-applications/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(WaitingListApplicationUpdateRequest())
        }.andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["PRISON"])
      fun `Update waiting list application (ROLE_PRISON) - 403`() {
        mockMvcWithSecurity.patch("/waiting-list-applications/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(WaitingListApplicationUpdateRequest())
        }.andExpect { status { isForbidden() } }
      }
    }
  }

  @Nested
  @DisplayName("Authorization tests for adding prisoner to multiple activities")
  inner class AuthorizationTestsForAddingPrisonerToMultipleActivities {
    private val prisonCode = RISLEY_PRISON_CODE
    private val prisonerNumber = "123456"
    private val principalName = "USERNAME"

    private fun createPrisonerWaitingListRequest(size: Int): List<PrisonerWaitingListApplicationRequest> = List(size) { index ->
      PrisonerWaitingListApplicationRequest(
        activityScheduleId = index + 1L,
        applicationDate = TimeSource.today(),
        requestedBy = "Requester",
        comments = "Testing",
        status = WaitingListStatus.PENDING,
      )
    }

    @Test
    fun `204 response when adding prisoner to activities`() {
      val requestList = createPrisonerWaitingListRequest(1)
      doNothing().whenever(waitingListService).addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, principalName)

      mockMvc.addToWaitingListApplication(prisonCode, prisonerNumber, requestList, includePrincipal = true).andExpect { status { isNoContent() } }

      verify(waitingListService).addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, principalName)
    }

    @Test
    @WithMockUser(roles = ["ACTIVITY_ADMIN"])
    fun `400 response when adding prisoner to more than 5 activities`() {
      val requestList = createPrisonerWaitingListRequest(6)

      doThrow(IllegalArgumentException("A maximum of 5 waiting list application requests can be submitted at once"))
        .whenever(waitingListService)
        .addPrisonerToMultipleActivities(anyString(), anyString(), anyList(), anyString())

      mockMvc.addToWaitingListApplication(prisonCode, prisonerNumber, requestList)
        .andExpect { status { isBadRequest() } }

      verify(waitingListService, times(1)).addPrisonerToMultipleActivities(any(), any(), any(), any())
    }

    @Test
    fun `401 response when user is not authorized`() {
      val requestList = listOf(mock<PrisonerWaitingListApplicationRequest>())
      mockMvcWithSecurity.addToWaitingListApplication(prisonCode, prisonerNumber, requestList, includePrincipal = false).andExpect { status { isUnauthorized() } }
      verify(waitingListService, never()).addPrisonerToMultipleActivities(any(), any(), any(), any())
    }

    @Test
    @WithMockUser(roles = ["PRISON"])
    fun `403 response when user role is invalid`() {
      val requestList = listOf(mock<PrisonerWaitingListApplicationRequest>())
      mockMvcWithSecurity.addToWaitingListApplication(prisonCode, prisonerNumber, requestList).andExpect { status { isForbidden() } }
      verify(waitingListService, never()).addPrisonerToMultipleActivities(any(), any(), any(), any())
    }

    @Test
    fun `404 response when activity schedule not found`() {
      val requestList = createPrisonerWaitingListRequest(1)
      doThrow(EntityNotFoundException("Activity schedule 1 not found"))
        .whenever(waitingListService)
        .addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, principalName)

      mockMvc.addToWaitingListApplication(prisonCode, prisonerNumber, requestList, true).andExpect { status { isNotFound() } }
      verify(waitingListService).addPrisonerToMultipleActivities(prisonCode, prisonerNumber, requestList, principalName)
    }
  }

  @Nested
  @DisplayName("Authorization tests for retrieving waiting list application history")
  inner class AuthorizationTestsForWaitingListApplicationHistory {
    private val waitingListId = 100L

    @Test
    @WithMockUser(roles = ["PRISON"])
    fun `200 response when user role is valid`() {
      whenever(waitingListService.getWaitingListHistoryBy(waitingListId)).thenReturn(emptyList())
      mockMvcWithSecurity.getWaitingListHistory(waitingListId, true).andExpect { status { isOk() } }
      verify(waitingListService).getWaitingListHistoryBy(waitingListId)
    }

    @Test
    fun `401 response when user is not authorized`() {
      mockMvcWithSecurity.getWaitingListHistory(waitingListId, false)
        .andExpect { status { isUnauthorized() } }
      verify(waitingListService, never()).getWaitingListHistoryBy(any())
    }

    @Test
    @WithMockUser(roles = ["INVALID_ROLE"])
    fun `403 response when user role is invalid`() {
      mockMvcWithSecurity.getWaitingListHistory(waitingListId).andExpect { status { isForbidden() } }
      verify(waitingListService, never()).getWaitingListHistoryBy(any())
    }

    @Test
    fun `404 response when waiting list for specified ID is not found`() {
      doThrow(EntityNotFoundException("Waiting list application for 100 not found"))
        .whenever(waitingListService).getWaitingListHistoryBy(waitingListId)
      mockMvc.getWaitingListHistory(waitingListId, true).andExpect {
        status { isNotFound() }
        jsonPath("$.developerMessage") { value("Waiting list application for 100 not found") }
        jsonPath("$.userMessage") { value("Not found: Waiting list application for 100 not found") }
        verify(waitingListService).getWaitingListHistoryBy(waitingListId)
      }
    }
  }

  private fun MockMvc.updatedWaitingList(id: Long, request: WaitingListApplicationUpdateRequest, user: Principal) = mockMvc.patch("/waiting-list-applications/$id") {
    principal = user
    accept = MediaType.APPLICATION_JSON
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(request)
  }

  private fun MockMvc.addToWaitingListApplication(prisonCode: String, prisonerNumber: String, request: List<PrisonerWaitingListApplicationRequest>, includePrincipal: Boolean = true) = post("/waiting-list-applications/$prisonCode/prisoner/$prisonerNumber") {
    if (includePrincipal) {
      principal = Principal { "USERNAME" }
    }
    content = mapper.writeValueAsString(request)
    contentType = MediaType.APPLICATION_JSON
  }

  private fun MockMvc.getWaitingListHistory(waitingListId: Long, includePrincipal: Boolean = true) = get("/waiting-list-applications/$waitingListId/history") {
    if (includePrincipal) {
      principal = Principal { "USERNAME" }
    }
  }
}
