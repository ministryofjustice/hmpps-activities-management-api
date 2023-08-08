package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel

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
}
