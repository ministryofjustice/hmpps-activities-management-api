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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentDetailService

@WebMvcTest(controllers = [AppointmentDetailController::class])
@ContextConfiguration(classes = [AppointmentDetailController::class])
class AppointmentDetailControllerTest : ControllerTestBase<AppointmentDetailController>() {
  @MockBean
  private lateinit var appointmentDetailService: AppointmentDetailService

  override fun controller() = AppointmentDetailController(appointmentDetailService)

  @Test
  fun `200 response when get appointment detail by valid id`() {
    val appointmentDetail = appointmentDetail()

    whenever(appointmentDetailService.getAppointmentDetailById(1)).thenReturn(appointmentDetail)

    val response = mockMvc.getAppointmentDetailById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    Assertions.assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentDetail))

    verify(appointmentDetailService).getAppointmentDetailById(1)
  }

  @Test
  fun `404 response when get appointment detail by invalid id`() {
    whenever(appointmentDetailService.getAppointmentDetailById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentDetailById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    Assertions.assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentDetailService).getAppointmentDetailById(-1)
  }

  private fun MockMvc.getAppointmentDetailById(id: Long) = get("/appointment-details/{appointmentId}", id)
}
