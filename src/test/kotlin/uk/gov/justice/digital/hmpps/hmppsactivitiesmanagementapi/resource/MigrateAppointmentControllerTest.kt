package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentService

@WebMvcTest(controllers = [MigrateAppointmentController::class])
@ContextConfiguration(classes = [MigrateAppointmentController::class])
class MigrateAppointmentControllerTest : ControllerTestBase<MigrateAppointmentController>() {

  @MockBean
  private lateinit var appointmentService: AppointmentService

  override fun controller() = MigrateAppointmentController(appointmentService)

  @BeforeEach
  fun resetMocks() {
    reset(appointmentService)
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests() {
    @Nested
    @DisplayName("Migrate appointments")
    inner class MigrateAppointmentsTests() {
      private val migrateAppointment = appointmentMigrateRequest()

      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Migrate appointment (ROLE_NOMIS_APPOINTMENTS) - 201`() {
        mockMvcWithSecurity.post("/migrate-appointment") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(migrateAppointment)
        }.andExpect { status { isCreated() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Migrate appointment (ROLE_NOMIS_ACTIVITIES) - 403`() {
        mockMvcWithSecurity.post("/migrate-appointment") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(migrateAppointment)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Migrate appointment (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.post("/migrate-appointment") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(migrateAppointment)
        }.andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Migrate appointment (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.post("/migrate-appointment") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(migrateAppointment)
        }.andExpect { status { isForbidden() } }
      }
    }
  }
}
