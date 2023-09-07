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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchResultModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentService
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [AppointmentOccurrenceController::class])
@ContextConfiguration(classes = [AppointmentOccurrenceController::class])
class AppointmentOccurrenceControllerTest : ControllerTestBase<AppointmentOccurrenceController>() {
  @MockBean
  private lateinit var appointmentService: AppointmentService

  @MockBean
  private lateinit var appointmentSearchService: AppointmentSearchService

  override fun controller() = AppointmentOccurrenceController(
    appointmentService,
    appointmentSearchService,
  )

  @Test
  fun `404 not found response when update appointment occurrence by invalid id`() {
    val request = AppointmentUpdateRequest()
    val mockPrincipal: Principal = mock()

    whenever(appointmentService.updateAppointment(-1, request, mockPrincipal)).thenThrow(EntityNotFoundException("Appointment Occurrence -1 not found"))

    val response = mockMvc.updateAppointmentOccurrence(-1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment Occurrence -1 not found")

    verify(appointmentService).updateAppointment(-1, request, mockPrincipal)
  }

  @Test
  fun `400 bad request response when update appointment occurrence with invalid json`() {
    val request = AppointmentUpdateRequest(
      inCell = false,
      startDate = LocalDate.now().minusDays(1),
      startTime = LocalTime.of(10, 30),
      endTime = LocalTime.of(10, 0),
      addPrisonerNumbers = emptyList(),
    )
    val mockPrincipal: Principal = mock()

    mockMvc.updateAppointmentOccurrence(1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Internal location id must be supplied if in cell = false"))
            value(Matchers.containsString("Start date must not be in the past"))
            value(Matchers.containsString("End time must be after the start time"))
          }
        }
      }

    verifyNoInteractions(appointmentService)
  }

  @Test
  fun `202 accepted response when update appointment occurrence with valid json`() {
    val request = AppointmentUpdateRequest()
    val expectedResponse = appointmentSeriesEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentService.updateAppointment(1, request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.updateAppointmentOccurrence(1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  @Test
  fun `400 bad request response when search appointment occurrences with invalid json`() {
    val request = AppointmentSearchRequest()
    val mockPrincipal: Principal = mock()

    mockMvc.searchAppointmentOccurrences("TPR", request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Start date must be supplied"))
          }
        }
      }

    verifyNoInteractions(appointmentService)
  }

  @Test
  fun `202 accepted response when search appointment occurrences with valid json`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now())
    val expectedResponse = listOf(appointmentOccurrenceSearchResultModel())

    val mockPrincipal: Principal = mock()

    whenever(appointmentSearchService.searchAppointments("TPR", request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.searchAppointmentOccurrences("TPR", request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  private fun MockMvc.updateAppointmentOccurrence(id: Long, request: AppointmentUpdateRequest, principal: Principal) =
    patch("/appointment-occurrences/{appointmentOccurrenceId}", id) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }

  private fun MockMvc.searchAppointmentOccurrences(prisonCode: String, request: AppointmentSearchRequest, principal: Principal) =
    post("/appointment-occurrences/{prisonCode}/search", prisonCode) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }
}
