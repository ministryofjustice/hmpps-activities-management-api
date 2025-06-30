package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
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
import java.time.LocalTime

@WebMvcTest(controllers = [AttendanceController::class])
@ContextConfiguration(classes = [AttendanceController::class])
class AttendanceControllerTest : ControllerTestBase<AttendanceController>() {

  @MockitoBean
  private lateinit var attendancesService: AttendancesService

  override fun controller() = AttendanceController(attendancesService)

  @Nested
  inner class SuspendedPrisonerAttendance {

    @BeforeEach
    fun `init`() {
      whenever(
        attendancesService.getSuspendedPrisonerAttendance(
          prisonCode = "MDI",
          date = LocalDate.now(),
        ),
      ).thenReturn(emptyList())
    }

    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_PRISON"])
    @Test
    fun `200 response`() {
      mockMvcWithSecurity.perform(
        MockMvcRequestBuilders.get("/attendances/MDI/suspended?date=${LocalDate.now()}")
          .header("Content-Type", "application/json"),
      ).andExpect(MockMvcResultMatchers.status().isOk)
    }
  }

  @Nested
  inner class GetPrisonerAttendance {
    val caseNotesApiClient: CaseNotesApiClient = mock()
    val prisonerNumber = "A1234AA"
    val prisonCode = "MDI"
    val attendanceEntity = Attendance(
      scheduledInstance = ScheduledInstance(
        scheduledInstanceId = 1234L,
        activitySchedule = ActivitySchedule(
          activity = Activity(
            activityId = 1234,
            prisonCode = prisonCode,
            activityCategory = ActivityCategory(
              activityCategoryId = 1234L,
              code = "CHAP",
              name = "Chaplaincy",
              description = "Chaplaincy",
            ),
            activityTier = EventTier(
              code = "ABCD",
              description = "Description",
            ),
            attendanceRequired = true,
            inCell = false,
            onWing = true,
            offWing = false,
            pieceWork = false,
            outsideWork = false,
            payPerSession = PayPerSession.H,
            summary = "Summary",
            description = "Description",
            startDate = LocalDate.now(),
            riskLevel = "High",
            createdTime = LocalDateTime.now(),
            createdBy = "Joe Bloggs",
            updatedTime = LocalDateTime.now(),
            updatedBy = "Joe Bloggs",
            isPaid = true,
          ),
          description = "description",
          capacity = 10,
          startDate = LocalDate.now(),
          scheduleWeeks = 1,
        ),
        sessionDate = LocalDate.now(),
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusHours(1),
        timeSlot = TimeSlot.AM,
      ),
      prisonerNumber = prisonerNumber,
    )

    @Test
    fun `200 response when get attendance for prisoner found`() {
      val history = AttendanceHistory(attendanceHistoryId = 1L, attendance = attendanceEntity, recordedTime = LocalDateTime.now(), recordedBy = "TEST_USER", issuePayment = true, incentiveLevelWarningIssued = false)
      attendanceEntity.addHistory(history)
      val attendance = transform(attendanceEntity, caseNotesApiClient, true)
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))).thenReturn(listOf(attendance))

      val response = mockMvc.get("/attendances/prisoner/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(attendance)))

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    }

    @Test
    fun `200 response when get attendance for prisoner found including prison code`() {
      val history = AttendanceHistory(attendanceHistoryId = 1L, attendance = attendanceEntity, recordedTime = LocalDateTime.now(), recordedBy = "TEST_USER", issuePayment = true, incentiveLevelWarningIssued = false)
      attendanceEntity.addHistory(history)
      val attendance = transform(attendanceEntity, caseNotesApiClient, true)
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1), prisonCode = prisonCode)).thenReturn(listOf(attendance))

      val response = mockMvc.get("/attendances/prisoner/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}&prisonCode=$prisonCode")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(attendance)))

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1), prisonCode = prisonCode)
    }

    @Test
    fun `404 response when get attendance for prisoner not found`() {
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.get("/attendances/prisoner/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("not found")

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    }
  }

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
    val attendanceEntity = attendance()
    val history = AttendanceHistory(attendanceHistoryId = 1L, attendance = attendanceEntity, recordedTime = LocalDateTime.now(), recordedBy = "TEST_USER", issuePayment = true, incentiveLevelWarningIssued = false)
    attendanceEntity.addHistory(history)
    val attendance = transform(attendanceEntity, caseNotesApiClient, true)

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

  private fun MockMvc.getAttendanceById(attendanceId: String) = get("/attendances/$attendanceId")

  private fun MockMvc.getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate) = get("/attendances/$prisonCode/$sessionDate")
}
