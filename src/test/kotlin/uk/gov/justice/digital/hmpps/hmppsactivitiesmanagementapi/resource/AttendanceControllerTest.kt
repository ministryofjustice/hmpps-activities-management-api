package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [AttendanceController::class])
@ContextConfiguration(classes = [AttendanceController::class])
class AttendanceControllerTest : ControllerTestBase<AttendanceController>() {

  @MockBean
  private lateinit var attendancesService: AttendancesService

  override fun controller() = AttendanceController(attendancesService)

  @Test
  fun `204 response when mark attendance records`() {
    val mockPrincipal: Principal = mock()
    mockMvc.put("/attendances") {
      principal = mockPrincipal
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        listOf(
          AttendanceUpdateRequest(1, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null),
          AttendanceUpdateRequest(2, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "SICK", null, null, null, null, null),
        ),
      )
    }
      .andExpect { status { isNoContent() } }

    verify(attendancesService).mark(
      "",
      listOf(
        AttendanceUpdateRequest(1, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null),
        AttendanceUpdateRequest(2, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "SICK", null, null, null, null, null),
      ),
    )
  }

  @Test
  fun `200 response when get attendance by ID found`() {
    val caseNotesApiClient: CaseNotesApiClient = mock()
    val attendance = transform(attendance(), caseNotesApiClient)

    whenever(attendancesService.getAttendanceById(1)).thenReturn(attendance)

    val response = mockMvc.getAttendanceById("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendance))

    verify(attendancesService).getAttendanceById(1)
  }

  @Test
  fun `404 response when get attendance by ID not found`() {
    whenever(attendancesService.getAttendanceById(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getAttendanceById("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(attendancesService).getAttendanceById(2)
  }

  @Test
  fun `200 response when get attendance list by date found`() {
    val attendanceList = attendanceList().toModel()

    whenever(attendancesService.getAllAttendanceByDate(PENTONVILLE_PRISON_CODE, LocalDate.now())).thenReturn(attendanceList)

    val response = mockMvc.getAllAttendanceByDate(PENTONVILLE_PRISON_CODE, LocalDate.now())
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendanceList))

    verify(attendancesService).getAllAttendanceByDate(PENTONVILLE_PRISON_CODE, LocalDate.now())
  }

  private fun MockMvc.getAttendanceById(attendanceId: String) =
    get("/attendances/$attendanceId")

  private fun MockMvc.getAttendanceSummaryByDate(prisonCode: String, sessionDate: LocalDate) =
    get("/attendances/summary/$prisonCode/$sessionDate")

  private fun MockMvc.getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate) =
    get("/attendances/$prisonCode/$sessionDate")

  companion object {
    val caseNote = CaseNote(
      caseNoteId = "1",
      offenderIdentifier = "A1234AA",
      type = "NEG",
      typeDescription = "Negative",
      subType = "sub type",
      subTypeDescription = "sub type description",
      source = "source",
      creationDateTime = LocalDateTime.now(),
      occurrenceDateTime = LocalDateTime.now(),
      authorName = "author",
      authorUserId = "author id",
      text = "Case Note Text",
      eventId = 1,
      sensitive = false,
    )
  }
}
