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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchResultModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentService
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [AppointmentController::class])
@ContextConfiguration(classes = [AppointmentController::class])
class AppointmentControllerTest : ControllerTestBase<AppointmentController>() {
  @MockBean
  private lateinit var appointmentService: AppointmentService

  @MockBean
  private lateinit var appointmentSearchService: AppointmentSearchService

  override fun controller() = AppointmentController(
    appointmentService,
    appointmentSearchService,
  )

  @Test
  fun `200 response when get appointment by valid id`() {
    val model = appointmentModel()

    whenever(appointmentService.getAppointmentById(1)).thenReturn(model)

    val response = mockMvc.getAppointmentById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(model))

    verify(appointmentService).getAppointmentById(1)
  }

  @Test
  fun `404 response when get appointment by invalid id`() {
    whenever(appointmentService.getAppointmentById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not Found")

    verify(appointmentService).getAppointmentById(-1)
  }

  @Test
  fun `200 response when get appointment details by valid id`() {
    val appointmentDetails = appointmentDetails()

    whenever(appointmentService.getAppointmentDetailsById(1)).thenReturn(appointmentDetails)

    val response = mockMvc.getAppointmentDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentDetails))

    verify(appointmentService).getAppointmentDetailsById(1)
  }

  @Test
  fun `404 response when get appointment details by invalid id`() {
    whenever(appointmentService.getAppointmentDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not Found")

    verify(appointmentService).getAppointmentDetailsById(-1)
  }

  @Test
  fun `404 not found response when update appointment by invalid id`() {
    val request = AppointmentUpdateRequest()
    val mockPrincipal: Principal = mock()

    whenever(appointmentService.updateAppointment(-1, request, mockPrincipal)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.updateAppointment(-1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not Found")

    verify(appointmentService).updateAppointment(-1, request, mockPrincipal)
  }

  @Test
  fun `400 bad request response when update appointment with invalid json`() {
    val request = AppointmentUpdateRequest(
      inCell = false,
      startDate = LocalDate.now().minusDays(1),
      startTime = LocalTime.of(10, 30),
      endTime = LocalTime.of(10, 0),
      addPrisonerNumbers = emptyList(),
    )
    val mockPrincipal: Principal = mock()

    mockMvc.updateAppointment(1, request, mockPrincipal)
      .andDo { print() }
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

    verifyNoInteractions(appointmentService)
  }

  @Test
  fun `202 accepted response when update appointment with valid json`() {
    val request = AppointmentUpdateRequest()
    val expectedResponse = appointmentSeriesEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentService.updateAppointment(1, request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.updateAppointment(1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  @Test
  fun `400 bad request response when search appointments with invalid json`() {
    val request = AppointmentSearchRequest()
    val mockPrincipal: Principal = mock()

    mockMvc.searchAppointments("TPR", request, mockPrincipal)
      .andDo { print() }
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

    verifyNoInteractions(appointmentService)
  }

  @Test
  fun `202 accepted response when search appointments with valid json`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now())
    val expectedResponse = listOf(appointmentSearchResultModel())

    val mockPrincipal: Principal = mock()

    whenever(appointmentSearchService.searchAppointments("TPR", request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.searchAppointments("TPR", request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  private fun MockMvc.getAppointmentById(id: Long) = get("/appointments/{appointmentId}", id)

  private fun MockMvc.getAppointmentDetailsById(id: Long) = get("/appointments/{appointmentId}/details", id)

  private fun MockMvc.updateAppointment(id: Long, request: AppointmentUpdateRequest, principal: Principal) =
    patch("/appointments/{appointmentId}", id) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }

  private fun MockMvc.searchAppointments(prisonCode: String, request: AppointmentSearchRequest, principal: Principal) =
    post("/appointments/{prisonCode}/search", prisonCode) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }
}
