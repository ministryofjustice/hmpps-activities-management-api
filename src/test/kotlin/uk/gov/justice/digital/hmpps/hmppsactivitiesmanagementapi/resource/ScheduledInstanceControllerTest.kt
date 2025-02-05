package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary.AttendanceSummaryDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [ScheduledInstanceController::class])
@ContextConfiguration(classes = [ScheduledInstanceController::class])
class ScheduledInstanceControllerTest : ControllerTestBase<ScheduledInstanceController>() {

  @MockitoBean
  private lateinit var scheduledInstanceService: ScheduledInstanceService

  override fun controller() = ScheduledInstanceController(scheduledInstanceService)

  @Test
  fun `200 response when get instance by ID found`() {
    val instance = activityEntity().schedules().first().instances().first().toModel()

    whenever(scheduledInstanceService.getActivityScheduleInstanceById(1)).thenReturn(instance)

    val response = mockMvc.getScheduledInstanceById("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(instance))

    verify(scheduledInstanceService).getActivityScheduleInstanceById(1)
  }

  @Test
  fun `404 response when get instance by ID not found`() {
    whenever(scheduledInstanceService.getActivityScheduleInstanceById(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getScheduledInstanceById("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(scheduledInstanceService).getActivityScheduleInstanceById(2)
  }

  @Test
  fun `200 response when get attendees by instance ID found`() {
    val attendees = listOf(
      ScheduledAttendee(
        scheduledInstanceId = 1,
        allocationId = 2,
        prisonerNumber = "ABC123",
        bookingId = 100001,
        suspended = false,
      ),
    )

    whenever(scheduledInstanceService.getAttendeesForScheduledInstance(1)).thenReturn(attendees)

    val response = mockMvc.getScheduledAttendeesByScheduledInstance("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendees))

    verify(scheduledInstanceService).getAttendeesForScheduledInstance(1)
  }

  @Test
  fun `404 response when get attendees by instance by ID not found`() {
    whenever(scheduledInstanceService.getAttendeesForScheduledInstance(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getScheduledAttendeesByScheduledInstance("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(scheduledInstanceService).getAttendeesForScheduledInstance(2)
  }

  @Test
  fun `204 response when successfully cancelling scheduled instance`() {
    mockMvc.put("/scheduled-instances/1/cancel") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", null),
      )
    }.andExpect { status { isNoContent() } }

    verify(scheduledInstanceService).cancelScheduledInstance(
      1,
      ScheduleInstanceCancelRequest("Staff unavailable", "USER1", null),
    )
  }

  @Test
  fun `404 response when scheduled instance to be cancelled is not found`() {
    whenever(
      scheduledInstanceService.cancelScheduledInstance(
        2,
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", null),
      ),
    ).thenThrow(EntityNotFoundException("not found"))

    mockMvc.put("/scheduled-instances/2/cancel") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", null),
      )
    }.andExpect { status { isNotFound() } }
  }

  @Test
  fun `400 response when bad request`() {
    whenever(
      scheduledInstanceService.cancelScheduledInstance(
        3,
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", null),
      ),
    ).thenThrow(IllegalArgumentException("Bad request"))

    mockMvc.put("/scheduled-instances/3/cancel") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        ScheduleInstanceCancelRequest("Staff unavailable", "USER1", null),
      )
    }.andExpect { status { isBadRequest() } }
  }

  @Nested
  @DisplayName("Attendance Summary")
  inner class AttendanceSummaryTests {
    @Test
    fun `200 response when request succeeds`() {
      val now = LocalDate.now()

      val summaries = listOf(
        ScheduledInstanceAttendanceSummary(
          scheduledInstanceId = 1,
          activityId = 2,
          activityScheduleId = 4,
          summary = "Maths",
          categoryId = 5,
          sessionDate = now,
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(11, 0),
          inCell = true,
          onWing = true,
          offWing = true,
          attendanceRequired = true,
          cancelled = false,
          timeSlot = TimeSlot.AM,
          attendanceSummary = AttendanceSummaryDetails(
            allocations = 10,
          ),
        ),
      )

      whenever(scheduledInstanceService.attendanceSummary("RSI", now)).thenReturn(summaries)

      val response = mockMvc.getAttendancesSummary("RSI", now)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(summaries))

      verify(scheduledInstanceService).attendanceSummary("RSI", now)
    }

    @Test
    fun `400 response bad request exception is thrown`() {
      whenever(
        scheduledInstanceService.attendanceSummary(any(), any()),
      ).thenThrow(IllegalArgumentException("Bad request"))

      val response = mockMvc.getAttendancesSummary("RSI", LocalDate.now())
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `404 response not found exception is thrown`() {
      whenever(
        scheduledInstanceService.attendanceSummary(any(), any()),
      ).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.getAttendancesSummary("RSI", LocalDate.now())
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
    }
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests {
    @Nested
    @DisplayName("Get Schedule instance by id")
    inner class GetScheduleInstanceById {
      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Get schedule instance by id (ROLE_NOMIS_ACTIVITIES) - 200`() {
        mockMvcWithSecurity.get("/scheduled-instances/1") {
          contentType = MediaType.APPLICATION_JSON
          header(CASELOAD_ID, "MDI")
        }.andExpect { status { isOk() } }
      }
    }
  }

  private fun MockMvc.getScheduledInstanceById(instanceId: String) = get("/scheduled-instances/$instanceId")

  private fun MockMvc.getScheduledAttendeesByScheduledInstance(instanceId: String) = get("/scheduled-instances/$instanceId/scheduled-attendees")

  private fun MockMvc.getAttendancesSummary(prisonCode: String, date: LocalDate) = get("/scheduled-instances/attendance-summary?prisonCode=$prisonCode&date=$date")
}
