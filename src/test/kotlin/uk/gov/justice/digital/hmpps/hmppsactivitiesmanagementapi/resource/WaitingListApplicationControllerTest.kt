package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
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

  private fun MockMvc.updatedWaitingList(id: Long, request: WaitingListApplicationUpdateRequest, user: Principal) =
    mockMvc.patch("/waiting-list-applications/$id") {
      principal = user
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(request)
    }
}
