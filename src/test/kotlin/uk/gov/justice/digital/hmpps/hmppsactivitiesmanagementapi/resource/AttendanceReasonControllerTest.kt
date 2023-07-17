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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendanceReasonService

@WebMvcTest(controllers = [AttendanceReasonController::class])
@ContextConfiguration(classes = [AttendanceReasonController::class])
class AttendanceReasonControllerTest : ControllerTestBase<AttendanceReasonController>() {

  @MockBean
  private lateinit var attendanceReasonService: AttendanceReasonService

  override fun controller() = AttendanceReasonController(attendanceReasonService)

  @Test
  fun `200 response when get attendance reasons`() {
    val expectedModel = listOf(attendanceReason().toModel())

    whenever(attendanceReasonService.getAll()).thenReturn(listOf(attendanceReason().toModel()))

    val response = mockMvc
      .get("/attendance-reasons")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(attendanceReasonService).getAll()
  }
}
