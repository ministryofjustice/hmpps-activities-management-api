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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceDetailsService

@WebMvcTest(controllers = [AppointmentOccurrenceDetailsController::class])
@ContextConfiguration(classes = [AppointmentOccurrenceDetailsController::class])
class AppointmentOccurrenceDetailsControllerTest : ControllerTestBase<AppointmentOccurrenceDetailsController>() {
  @MockBean
  private lateinit var appointmentOccurrenceDetailsService: AppointmentOccurrenceDetailsService

  override fun controller() = AppointmentOccurrenceDetailsController(appointmentOccurrenceDetailsService)

  @Test
  fun `200 response when get appointment occurrence details by valid id`() {
    val appointmentOccurrenceDetails = appointmentOccurrenceDetails()

    whenever(appointmentOccurrenceDetailsService.getAppointmentOccurrenceDetailsById(1)).thenReturn(appointmentOccurrenceDetails)

    val response = mockMvc.getAppointmentOccurrenceDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentOccurrenceDetails))

    verify(appointmentOccurrenceDetailsService).getAppointmentOccurrenceDetailsById(1)
  }

  @Test
  fun `404 response when get appointment occurrence details by invalid id`() {
    whenever(appointmentOccurrenceDetailsService.getAppointmentOccurrenceDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment Occurrence -1 not found"))

    val response = mockMvc.getAppointmentOccurrenceDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment Occurrence -1 not found")

    verify(appointmentOccurrenceDetailsService).getAppointmentOccurrenceDetailsById(-1)
  }

  private fun MockMvc.getAppointmentOccurrenceDetailsById(id: Long) = get("/appointment-occurrence-details/{appointmentOccurrenceId}", id)
}
