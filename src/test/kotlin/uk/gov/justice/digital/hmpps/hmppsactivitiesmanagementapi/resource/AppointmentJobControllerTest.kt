package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAppointmentAttendeesJob

@WebMvcTest(controllers = [AppointmentJobController::class])
@ContextConfiguration(classes = [AppointmentJobController::class])
class AppointmentJobControllerTest : ControllerTestBase<AppointmentJobController>() {
  @MockBean
  private lateinit var manageAppointmentAttendeesJob: ManageAppointmentAttendeesJob

  override fun controller() = AppointmentJobController(manageAppointmentAttendeesJob)

  @Test
  fun `202 response when manage appointment attendees job started`() {
    val response = mockMvc.post("/job/appointments/manage-attendees?daysBeforeNow=1&daysAfterNow=2") {
      contentType = MediaType.APPLICATION_JSON
    }.andExpect { status { isAccepted() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage appointment attendees job started")

    verify(manageAppointmentAttendeesJob).execute(1, 2)
  }
}
