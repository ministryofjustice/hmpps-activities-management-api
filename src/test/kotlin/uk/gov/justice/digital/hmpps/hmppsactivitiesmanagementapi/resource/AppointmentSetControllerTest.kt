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
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment.AppointmentSetController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSetService
import java.security.Principal

@WebMvcTest(controllers = [AppointmentSetController::class])
@ContextConfiguration(classes = [AppointmentSetController::class])
class AppointmentSetControllerTest : ControllerTestBase<AppointmentSetController>() {
  @MockitoBean
  private lateinit var appointmentSetService: AppointmentSetService

  override fun controller() = AppointmentSetController(appointmentSetService)

  @Test
  fun `200 response when get appointment set details by valid id`() {
    val details = appointmentSetDetails()

    whenever(appointmentSetService.getAppointmentSetDetailsById(1)).thenReturn(details)

    val response = mockMvc.getAppointmentSetDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(details))

    verify(appointmentSetService).getAppointmentSetDetailsById(1)
  }

  @Test
  fun `404 response when get appointment set details by invalid id`() {
    whenever(appointmentSetService.getAppointmentSetDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment Set -1 not found"))

    val response = mockMvc.getAppointmentSetDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment Set -1 not found")

    verify(appointmentSetService).getAppointmentSetDetailsById(-1)
  }

  @Test
  fun `create appointment set with empty json returns 400 bad request`() {
    mockMvc.post("/appointment-set") {
      contentType = MediaType.APPLICATION_JSON
      content = "{}"
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Prison code must be supplied"))
            value(Matchers.containsString("Category code must be supplied"))
            value(Matchers.containsString("Internal location id must be supplied if in cell = false"))
            value(Matchers.containsString("Start date must be supplied"))
            value(Matchers.containsString("At least one appointment must be supplied"))
          }
        }
      }

    verifyNoInteractions(appointmentSetService)
  }

  @Test
  fun `create appointment set with invalid appointments returns 400 bad request`() {
    mockMvc.post("/appointment-set") {
      contentType = MediaType.APPLICATION_JSON
      content = """{"appointments": [{}]}"""
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Prisoner number must be supplied"))
            value(Matchers.containsString("Start time must be supplied"))
            value(Matchers.containsString("End time must be supplied"))
          }
        }
      }

    verifyNoInteractions(appointmentSetService)
  }

  @Test
  fun `create appointment set with valid json returns 201 created and appointment set model`() {
    val request = appointmentSetCreateRequest()
    val expectedResponse = appointmentSetEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentSetService.createAppointmentSet(request, mockPrincipal)).thenReturn(expectedResponse)

    val response =
      mockMvc.post("/appointment-set") {
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

  private fun MockMvc.getAppointmentSetDetailsById(id: Long) = get("/appointment-set/{appointmentSetId}/details", id)
}
