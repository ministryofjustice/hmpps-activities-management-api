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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesDetailsService

@WebMvcTest(controllers = [AppointmentDetailsController::class])
@ContextConfiguration(classes = [AppointmentDetailsController::class])
class AppointmentDetailsControllerTest : ControllerTestBase<AppointmentDetailsController>() {
  @MockBean
  private lateinit var appointmentSeriesDetailsService: AppointmentSeriesDetailsService

  override fun controller() = AppointmentDetailsController(appointmentSeriesDetailsService)

  @Test
  fun `200 response when get appointment details by valid id`() {
    val appointmentDetails = appointmentSeriesDetails()

    whenever(appointmentSeriesDetailsService.getAppointmentSeriesDetailsById(1)).thenReturn(appointmentDetails)

    val response = mockMvc.getAppointmentDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentDetails))

    verify(appointmentSeriesDetailsService).getAppointmentSeriesDetailsById(1)
  }

  @Test
  fun `404 response when get appointment details by invalid id`() {
    whenever(appointmentSeriesDetailsService.getAppointmentSeriesDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentSeriesDetailsService).getAppointmentSeriesDetailsById(-1)
  }

  private fun MockMvc.getAppointmentDetailsById(id: Long) = get("/appointment-details/{appointmentId}", id)
}
