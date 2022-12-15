package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService

@WebMvcTest(controllers = [AttendanceController::class])
@ContextConfiguration(classes = [AttendanceController::class])
class AttendanceControllerTest : ControllerTestBase<AttendanceController>() {

  @MockBean
  private lateinit var attendancesService: AttendancesService

  override fun controller() = AttendanceController(attendancesService)

  @Test
  fun `204 response when mark attendance records`() {
    mockMvc.put("/attendances") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        listOf(
          AttendanceUpdateRequest(1, "ATT"),
          AttendanceUpdateRequest(2, "ABS")
        )
      )
    }
      .andExpect { status { isNoContent() } }

    verify(attendancesService).mark(
      listOf(
        AttendanceUpdateRequest(1, "ATT"),
        AttendanceUpdateRequest(2, "ABS")
      )
    )
  }
}
