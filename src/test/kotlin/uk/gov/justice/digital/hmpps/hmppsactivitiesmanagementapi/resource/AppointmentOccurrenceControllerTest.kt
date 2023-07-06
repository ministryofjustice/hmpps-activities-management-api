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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchResultModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceService
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [AppointmentOccurrenceController::class])
@ContextConfiguration(classes = [AppointmentOccurrenceController::class])
class AppointmentOccurrenceControllerTest : ControllerTestBase<AppointmentOccurrenceController>() {
  @MockBean
  private lateinit var appointmentOccurrenceService: AppointmentOccurrenceService

  @MockBean
  private lateinit var appointmentOccurrenceSearchService: AppointmentOccurrenceSearchService

  override fun controller() = AppointmentOccurrenceController(
    appointmentOccurrenceService,
    appointmentOccurrenceSearchService,
  )

  @Test
  fun `404 not found response when update appointment occurrence by invalid id`() {
    val request = AppointmentOccurrenceUpdateRequest()
    val mockPrincipal: Principal = mock()

    whenever(appointmentOccurrenceService.updateAppointmentOccurrence(-1, request, mockPrincipal)).thenThrow(EntityNotFoundException("Appointment Occurrence -1 not found"))

    val response = mockMvc.updateAppointmentOccurrence(-1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment Occurrence -1 not found")

    verify(appointmentOccurrenceService).updateAppointmentOccurrence(-1, request, mockPrincipal)
  }

  @Test
  fun `400 bad request response when update appointment occurrence with invalid json`() {
    val request = AppointmentOccurrenceUpdateRequest(
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

    verifyNoInteractions(appointmentOccurrenceService)
  }

  @Test
  fun `202 accepted response when update appointment occurrence with valid json`() {
    val request = AppointmentOccurrenceUpdateRequest()
    val expectedResponse = appointmentEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentOccurrenceService.updateAppointmentOccurrence(1, request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.updateAppointmentOccurrence(1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  @Test
  fun `400 bad request response when search appointment occurrences with invalid json`() {
    val request = AppointmentOccurrenceSearchRequest()
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

    verifyNoInteractions(appointmentOccurrenceService)
  }

  @Test
  fun `202 accepted response when search appointment occurrences with valid json`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now())
    val expectedResponse = listOf(appointmentOccurrenceSearchResultModel())

    val mockPrincipal: Principal = mock()

    whenever(appointmentOccurrenceSearchService.searchAppointmentOccurrences("TPR", request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.searchAppointmentOccurrences("TPR", request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  private fun MockMvc.updateAppointmentOccurrence(id: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal) =
    patch("/appointment-occurrences/{appointmentOccurrenceId}", id) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }

  private fun MockMvc.searchAppointmentOccurrences(prisonCode: String, request: AppointmentOccurrenceSearchRequest, principal: Principal) =
    post("/appointment-occurrences/{prisonCode}/search", prisonCode) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }
}
