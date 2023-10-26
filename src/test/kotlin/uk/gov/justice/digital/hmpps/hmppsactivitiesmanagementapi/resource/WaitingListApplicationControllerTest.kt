package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.security.Principal

@WebMvcTest(controllers = [WaitingListApplicationController::class])
@ContextConfiguration(classes = [WaitingListApplicationController::class])
class WaitingListApplicationControllerTest : ControllerTestBase<WaitingListApplicationController>() {

  @MockBean
  private lateinit var waitingListService: WaitingListService

  override fun controller(): WaitingListApplicationController = WaitingListApplicationController(waitingListService)

  @Test
  fun `200 response when get by ID found`() {
    val waitingListApplication = waitingList().toModel()

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
    val waitingListApplication = waitingList().toModel()

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
    val waitingListApplication = waitingList().toModel()
    val pagedResult = PageImpl(listOf(waitingListApplication))

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

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(pagedResult))
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests() {
    @Nested
    @DisplayName("Waiting list application")
    inner class WaitingListApplicationTests() {
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

  private fun MockMvc.updatedWaitingList(id: Long, request: WaitingListApplicationUpdateRequest, user: Principal) =
    mockMvc.patch("/waiting-list-applications/$id") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }
}
