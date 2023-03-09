package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository

@WebMvcTest(controllers = [AttendanceReasonController::class])
@ContextConfiguration(classes = [AttendanceReasonController::class])
class AttendanceReasonControllerTest : ControllerTestBase<AttendanceReasonController>() {

  @MockBean
  private lateinit var attendanceReasonRepository: AttendanceReasonRepository

  override fun controller() = AttendanceReasonController(attendanceReasonRepository)

  @Test
  fun `200 response when get attendance reasons`() {
    val expectedModel = listOf(
      AttendanceReason(
        id = 1,
        code = "reason code",
        description = "reason description",
        attended = false,
        capturePay = true,
        captureMoreDetail = true,
        captureCaseNote = false,
        captureIncentiveLevelWarning = false,
        captureOtherText = false,
        displayInAbsence = true,
        displaySequence = 1,
        notes = "reason notes",
      ),
    )

    whenever(attendanceReasonRepository.findAll()).thenReturn(listOf(attendanceReason()))

    val response = mockMvc
      .get("/attendance-reasons")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

    verify(attendanceReasonRepository, times(1)).findAll()
  }
}
