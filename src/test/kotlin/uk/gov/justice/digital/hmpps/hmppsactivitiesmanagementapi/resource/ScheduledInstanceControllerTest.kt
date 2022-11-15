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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import javax.persistence.EntityNotFoundException

@WebMvcTest(controllers = [ScheduledInstanceController::class])
@ContextConfiguration(classes = [ScheduledInstanceController::class])
class ScheduledInstanceControllerTest : ControllerTestBase<ScheduledInstanceController>() {

  @MockBean
  private lateinit var attendancesService: AttendancesService

  override fun controller() = ScheduledInstanceController(attendancesService)

  @Test
  fun `200 response when get attendances by schedule ID found`() {
    val attendances = activityEntity().schedules.first().instances.first().attendances.map { transform(it) }

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

  private fun MockMvc.getAttendancesByScheduledInstance(instanceId: String) =
    get("/scheduled-instances/$instanceId/attendances")
}
