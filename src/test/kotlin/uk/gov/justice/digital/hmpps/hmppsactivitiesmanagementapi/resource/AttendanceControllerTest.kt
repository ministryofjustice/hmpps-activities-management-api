package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceEntity
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
          AttendanceUpdateRequest(1, "ATTENDED", null, null, null, null, null),
          AttendanceUpdateRequest(2, "SICK", null, null, null, null, null),
        ),
      )
    }
      .andExpect { status { isNoContent() } }

    verify(attendancesService).mark(
      listOf(
        AttendanceUpdateRequest(1, "ATTENDED", null, null, null, null, null),
        AttendanceUpdateRequest(2, "SICK", null, null, null, null, null),
      ),
    )
  }

  @Test
  fun `200 response when get attendance by ID found`() {
    val attendance = attendanceEntity().toModel()

    whenever(attendancesService.getAttendanceById(1)).thenReturn(attendance)

    val response = mockMvc.getAttendanceById("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    Assertions.assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendance))

    verify(attendancesService).getAttendanceById(1)
  }

  @Test
  fun `404 response when get attendance by ID not found`() {
    whenever(attendancesService.getAttendanceById(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getAttendanceById("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    Assertions.assertThat(response.contentAsString).contains("Not found")

    verify(attendancesService).getAttendanceById(2)
  }

  private fun MockMvc.getAttendanceById(attendanceId: String) =
    get("/attendances/$attendanceId")
}
