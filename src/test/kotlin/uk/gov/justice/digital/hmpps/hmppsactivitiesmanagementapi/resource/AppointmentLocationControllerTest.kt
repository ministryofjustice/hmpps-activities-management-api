package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment.AppointmentLocationController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocation

@WebMvcTest(controllers = [AppointmentLocationController::class])
@ContextConfiguration(classes = [AppointmentLocationController::class])
class AppointmentLocationControllerTest : ControllerTestBase<AppointmentLocationController>() {

  @MockitoBean
  private lateinit var locationService: LocationService

  override fun controller() = AppointmentLocationController(locationService)

  @Test
  fun `200 response when get all appointment locations`() {
    val locations = listOf(appointmentLocation(1, MOORLAND_PRISON_CODE))

    whenever(locationService.getLocationsForAppointments(MOORLAND_PRISON_CODE)).thenReturn(locations)

    val response = mockMvc
      .get("/appointment-locations/{prisonCode}", MOORLAND_PRISON_CODE)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(locations.toAppointmentLocation()))
  }
}
