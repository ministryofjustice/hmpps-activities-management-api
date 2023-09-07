package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.security.Principal

@WebMvcTest(controllers = [AppointmentController::class])
@ContextConfiguration(classes = [AppointmentController::class])
class AppointmentControllerTest : ControllerTestBase<AppointmentController>() {
  @MockBean
  private lateinit var appointmentSeriesService: AppointmentSeriesService

  override fun controller() = AppointmentController(appointmentSeriesService)

  @Test
  fun `200 response when get appointment by valid id`() {
    val appointmentSeries = appointmentSeriesEntity().toModel()

    whenever(appointmentSeriesService.getAppointmentSeriesById(1)).thenReturn(appointmentSeries)

    val response = mockMvc.getAppointmentById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentSeries))

    verify(appointmentSeriesService).getAppointmentSeriesById(1)
  }

  @Test
  fun `404 response when get appointment by invalid id`() {
    whenever(appointmentSeriesService.getAppointmentSeriesById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentSeriesService).getAppointmentSeriesById(-1)
  }

  @Test
  fun `create appointment with empty json returns 400 bad request`() {
    mockMvc.post("/appointments") {
      contentType = MediaType.APPLICATION_JSON
      content = "{}"
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Category code must be supplied"))
            value(Matchers.containsString("Prison code must be supplied"))
            value(Matchers.containsString("Internal location id must be supplied if in cell = false"))
            value(Matchers.containsString("Start date must be supplied"))
            value(Matchers.containsString("Start time must be supplied"))
            value(Matchers.containsString("End time must be supplied"))
            value(Matchers.containsString("At least one prisoner number must be supplied"))
          }
        }
      }

    verifyNoInteractions(appointmentSeriesService)
  }

  @Test
  fun `create appointment with valid json returns 201 created and appointment model`() {
    val request = appointmentCreateRequest()
    val expectedResponse = appointmentSeriesEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentSeriesService.createAppointmentSeries(request, mockPrincipal)).thenReturn(expectedResponse)

    val response =
      mockMvc.post("/appointments") {
        principal = mockPrincipal
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          request,
        )
      }
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isCreated() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  private fun MockMvc.getAppointmentById(id: Long) = get("/appointments/{appointmentId}", id)
}
