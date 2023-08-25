package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform

@WebMvcTest(controllers = [ScheduledInstanceController::class])
@ContextConfiguration(classes = [ScheduledInstanceController::class])
class ScheduledInstanceControllerTest : ControllerTestBase<ScheduledInstanceController>() {

  @MockBean
  private lateinit var scheduledInstanceService: ScheduledInstanceService

  @MockBean
  private lateinit var attendancesService: AttendancesService

  override fun controller() = ScheduledInstanceController(scheduledInstanceService, attendancesService)

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
  fun `200 response when get attendances by schedule ID found`() {
    val attendances = activityEntity().schedules().first().instances().first().attendances.map { transform(it, null) }

    whenever(attendancesService.findAttendancesByScheduledInstance(1)).thenReturn(attendances)

    val response = mockMvc.getAttendancesByScheduledInstance("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendances))

    verify(attendancesService).findAttendancesByScheduledInstance(1)
  }

  @Test
  fun `404 response when get attendances by scheduled instance ID not found`() {
    whenever(attendancesService.findAttendancesByScheduledInstance(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getAttendancesByScheduledInstance("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(attendancesService).findAttendancesByScheduledInstance(2)
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
  @DisplayName("Authorization tests")
  inner class AuthorizationTests() {
    @Nested
    @DisplayName("Get Schedule instance by id")
    inner class GetScheduleInstanceById() {
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

  private fun MockMvc.getScheduledInstanceById(instanceId: String) =
    get("/scheduled-instances/$instanceId")

  private fun MockMvc.getAttendancesByScheduledInstance(instanceId: String) =
    get("/scheduled-instances/$instanceId/attendances")
}
