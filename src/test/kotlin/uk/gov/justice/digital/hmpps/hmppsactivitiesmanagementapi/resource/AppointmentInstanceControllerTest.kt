package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentInstanceService

@WebMvcTest(controllers = [AppointmentInstanceController::class])
@ContextConfiguration(classes = [AppointmentInstanceController::class])
class AppointmentInstanceControllerTest : ControllerTestBase<AppointmentInstanceController>() {
  @MockBean
  private lateinit var appointmentInstanceService: AppointmentInstanceService

  override fun controller() = AppointmentInstanceController(appointmentInstanceService)

  @Test
  fun `200 response when get appointment instance by valid id`() {
    val appointmentInstance = appointmentInstanceEntity().toModel()

    whenever(appointmentInstanceService.getAppointmentInstanceById(1)).thenReturn(appointmentInstance)

    val response = mockMvc.getAppointmentInstanceById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentInstance))

    verify(appointmentInstanceService).getAppointmentInstanceById(1)
  }

  @Test
  fun `404 response when get appointment instance by invalid id`() {
    whenever(appointmentInstanceService.getAppointmentInstanceById(-1)).thenThrow(EntityNotFoundException("Appointment Instance -1 not found"))

    val response = mockMvc.getAppointmentInstanceById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not Found")

    verify(appointmentInstanceService).getAppointmentInstanceById(-1)
  }

  private fun MockMvc.getAppointmentInstanceById(id: Long) = get("/appointment-instances/{appointmentInstanceId}", id)
}
