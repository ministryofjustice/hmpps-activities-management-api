package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.security.Principal

@WebMvcTest(controllers = [AppointmentSeriesController::class])
@ContextConfiguration(classes = [AppointmentSeriesController::class])
class AppointmentSeriesControllerTest : ControllerTestBase<AppointmentSeriesController>() {
  @MockBean
  private lateinit var appointmentSeriesService: AppointmentSeriesService

  override fun controller() = AppointmentSeriesController(appointmentSeriesService)

  @Test
  fun `200 response when get appointment series by valid id`() {
    val model = appointmentSeriesEntity().toModel()

    whenever(appointmentSeriesService.getAppointmentSeriesById(1)).thenReturn(model)

    val response = mockMvc.getAppointmentSeriesById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(model))

    verify(appointmentSeriesService).getAppointmentSeriesById(1)
  }

  @Test
  fun `404 response when get appointment series by invalid id`() {
    whenever(appointmentSeriesService.getAppointmentSeriesById(-1)).thenThrow(EntityNotFoundException("Appointment Series -1 not found"))

    val response = mockMvc.getAppointmentSeriesById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not Found")

    verify(appointmentSeriesService).getAppointmentSeriesById(-1)
  }

  @Test
  fun `200 response when get appointment series details by valid id`() {
    val appointmentSeriesDetails = appointmentSeriesDetails()

    whenever(appointmentSeriesService.getAppointmentSeriesDetailsById(1)).thenReturn(appointmentSeriesDetails)

    val response = mockMvc.getAppointmentSeriesDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentSeriesDetails))

    verify(appointmentSeriesService).getAppointmentSeriesDetailsById(1)
  }

  @Test
  fun `404 response when get appointment series details by invalid id`() {
    whenever(appointmentSeriesService.getAppointmentSeriesDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment Series -1 not found"))

    val response = mockMvc.getAppointmentSeriesDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not Found")

    verify(appointmentSeriesService).getAppointmentSeriesDetailsById(-1)
  }

  @Test
  fun `create appointment series with empty json returns 400 bad request`() {
    mockMvc.post("/appointment-series") {
      contentType = MediaType.APPLICATION_JSON
      content = "{}"
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Bad Request")
          }
        }
      }

    verifyNoInteractions(appointmentSeriesService)
  }

  @Test
  fun `create appointment series with valid json returns 201 created and appointment series model`() {
    val request = appointmentSeriesCreateRequest()
    val expectedResponse = appointmentSeriesEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentSeriesService.createAppointmentSeries(request, mockPrincipal)).thenReturn(expectedResponse)

    val response =
      mockMvc.post("/appointment-series") {
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

  private fun MockMvc.getAppointmentSeriesById(id: Long) = get("/appointment-series/{appointmentSeriesId}", id)

  private fun MockMvc.getAppointmentSeriesDetailsById(id: Long) = get("/appointment-series/{appointmentSeriesId}/details", id)
}
