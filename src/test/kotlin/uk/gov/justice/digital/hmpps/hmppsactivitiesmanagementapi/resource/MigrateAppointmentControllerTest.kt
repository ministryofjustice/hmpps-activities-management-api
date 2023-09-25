package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.DeleteMigratedAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentSeriesService
import java.time.LocalDate

@WebMvcTest(controllers = [MigrateAppointmentController::class])
@ContextConfiguration(classes = [MigrateAppointmentController::class])
class MigrateAppointmentControllerTest : ControllerTestBase<MigrateAppointmentController>() {

  @MockBean
  private lateinit var appointmentSeriesService: AppointmentSeriesService

  @MockBean
  private lateinit var deleteMigratedAppointmentsJob: DeleteMigratedAppointmentsJob

  override fun controller() = MigrateAppointmentController(appointmentSeriesService, deleteMigratedAppointmentsJob)

  @BeforeEach
  fun resetMocks() {
    reset(appointmentSeriesService)
  }

  @Nested@DisplayName("Migrate appointments")
  inner class MigrateAppointmentTests {
    @Nested
    @DisplayName("Authorization tests")
    inner class AuthorizationTests {
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

  @Nested
  @DisplayName("Delete migrated appointments")
  inner class DeleteMigrateAppointmentsTests {
    @Nested
    @DisplayName("Authorization tests")
    inner class AuthorizationTests {
      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Delete migrate appointment (ROLE_NOMIS_APPOINTMENTS) - 202`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=2023-09-25")
          .andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Migrate appointment (ROLE_NOMIS_ACTIVITIES) - 403`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=2023-09-25")
          .andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Migrate appointment (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=2023-09-25")
          .andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Migrate appointment (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=2023-09-25")
          .andExpect { status { isForbidden() } }
      }
    }

    @Test
    fun `202 response when category code supplied`() {
      mockMvc.delete("/migrate-appointment/MDI?startDate=2023-09-25&categoryCode=CHAP")
        .andExpect { status { isAccepted() } }

      verify(deleteMigratedAppointmentsJob).execute("MDI", LocalDate.of(2023, 9, 25), "CHAP")
    }

    @Test
    fun `202 response when category code is not supplied`() {
      mockMvc.delete("/migrate-appointment/PVI?startDate=2022-08-26")
        .andExpect { status { isAccepted() } }

      verify(deleteMigratedAppointmentsJob).execute("PVI", LocalDate.of(2022, 8, 26))
    }

    @Test
    fun `400 response when no date supplied`() {
      mockMvc.delete("/migrate-appointment/MDI")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Required request parameter 'startDate' for method parameter type LocalDate is not present")
            }
          }
        }

      verifyNoInteractions(deleteMigratedAppointmentsJob)
    }

    @Test
    fun `Internal location events summaries - 400 response when invalid date supplied`() {
      mockMvc.delete("/migrate-appointment/MDI?startDate=invalid")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Error converting 'startDate' (invalid): Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
            }
          }
        }

      verifyNoInteractions(deleteMigratedAppointmentsJob)
    }
  }
}
