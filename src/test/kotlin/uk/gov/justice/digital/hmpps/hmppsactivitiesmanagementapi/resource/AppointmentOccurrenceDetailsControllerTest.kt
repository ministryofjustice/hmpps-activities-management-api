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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentDetailsService

@WebMvcTest(controllers = [AppointmentOccurrenceDetailsController::class])
@ContextConfiguration(classes = [AppointmentOccurrenceDetailsController::class])
class AppointmentOccurrenceDetailsControllerTest : ControllerTestBase<AppointmentOccurrenceDetailsController>() {
  @MockBean
  private lateinit var appointmentDetailsService: AppointmentDetailsService

  override fun controller() = AppointmentOccurrenceDetailsController(appointmentDetailsService)

  @Test
  fun `200 response when get appointment occurrence details by valid id`() {
    val appointmentOccurrenceDetails = appointmentDetails()

    whenever(appointmentDetailsService.getAppointmentDetailsById(1)).thenReturn(appointmentOccurrenceDetails)

    val response = mockMvc.getAppointmentOccurrenceDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentOccurrenceDetails))

    verify(appointmentDetailsService).getAppointmentDetailsById(1)
  }

  @Test
  fun `404 response when get appointment occurrence details by invalid id`() {
    whenever(appointmentDetailsService.getAppointmentDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment Occurrence -1 not found"))

    val response = mockMvc.getAppointmentOccurrenceDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment Occurrence -1 not found")

    verify(appointmentDetailsService).getAppointmentDetailsById(-1)
  }

  private fun MockMvc.getAppointmentOccurrenceDetailsById(id: Long) = get("/appointment-occurrence-details/{appointmentOccurrenceId}", id)
}
