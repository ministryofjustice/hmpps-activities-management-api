package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduleReasonEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocation

@WebMvcTest(controllers = [AppointmentLocationController::class])
@ContextConfiguration(classes = [AppointmentLocationController::class])
class AppointmentLocationControllerTest : ControllerTestBase<AppointmentLocationController>() {

  @MockBean
  private lateinit var referenceCodeService: ReferenceCodeService

  override fun controller() = AppointmentLocationController(referenceCodeService)

  @Test
  fun `200 response when get all appointment locations`() {
    val locations = listOf(appointmentLocation(1, moorlandPrisonCode))

    whenever(referenceCodeService.getScheduleLocations(moorlandPrisonCode, ScheduleReasonEventType.APPOINTMENT)).thenReturn(locations)

    val response = mockMvc
      .get("/appointment-locations/{prisonCode}", moorlandPrisonCode)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(locations.toAppointmentLocation()))
  }
}
