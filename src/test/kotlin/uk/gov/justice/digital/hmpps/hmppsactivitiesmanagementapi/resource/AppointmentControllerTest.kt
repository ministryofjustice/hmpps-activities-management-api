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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentService

@WebMvcTest(controllers = [AppointmentController::class])
@ContextConfiguration(classes = [AppointmentController::class])
class AppointmentControllerTest : ControllerTestBase<AppointmentController>() {
  @MockBean
  private lateinit var appointmentService: AppointmentService

  override fun controller() = AppointmentController(appointmentService)

  @Test
  fun `200 response when get appointment by valid id`() {
    val appointment = appointmentEntity().toModel()

    whenever(appointmentService.getAppointmentById(1)).thenReturn(appointment)

    val response = mockMvc.getAppointmentById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    Assertions.assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointment))

    verify(appointmentService).getAppointmentById(1)
  }

  @Test
  fun `404 response when get appointment by invalid id`() {
    whenever(appointmentService.getAppointmentById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    Assertions.assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentService).getAppointmentById(-1)
  }

  private fun MockMvc.getAppointmentById(id: Long) = get("/appointments/{appointmentId}", id)
}
