package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.DeleteMigratedAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment.MigrateAppointmentController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.MigrateAppointmentService
import java.time.LocalDate

@WebMvcTest(controllers = [MigrateAppointmentController::class])
@ContextConfiguration(classes = [MigrateAppointmentController::class])
class MigrateAppointmentControllerTest : ControllerTestBase<MigrateAppointmentController>() {

  @MockitoBean
  private lateinit var migrateAppointmentService: MigrateAppointmentService

  @MockitoBean
  private lateinit var deleteMigratedAppointmentsJob: DeleteMigratedAppointmentsJob

  override fun controller() = MigrateAppointmentController(migrateAppointmentService, deleteMigratedAppointmentsJob)

  @BeforeEach
  fun resetMocks() {
    reset(migrateAppointmentService)
  }

  @Nested
  @DisplayName("Migrate appointments")
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
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=${LocalDate.now()}")
          .andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["MIGRATE_APPOINTMENTS"])
      fun `Migrate appointment summary (ROLE_MIGRATE_APPOINTMENTS) - 202`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=${LocalDate.now()}")
          .andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["NOMIS_ACTIVITIES"])
      fun `Migrate appointment (ROLE_NOMIS_ACTIVITIES) - 403`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=${LocalDate.now()}")
          .andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Migrate appointment (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=${LocalDate.now()}")
          .andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Migrate appointment (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.delete("/migrate-appointment/MDI?startDate=${LocalDate.now()}")
          .andExpect { status { isForbidden() } }
      }
    }

    @Test
    fun `202 response when category code supplied`() {
      val startDate = LocalDate.now()
      mockMvc.delete("/migrate-appointment/MDI?startDate=$startDate&categoryCode=CHAP")
        .andExpect { status { isAccepted() } }

      verify(deleteMigratedAppointmentsJob).execute("MDI", startDate, "CHAP")
    }

    @Test
    fun `202 response when category code is not supplied`() {
      val startDate = LocalDate.now()
      mockMvc.delete("/migrate-appointment/PVI?startDate=$startDate")
        .andExpect { status { isAccepted() } }

      verify(deleteMigratedAppointmentsJob).execute("PVI", startDate)
    }

    @Test
    fun `400 response when no start date supplied`() {
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
    fun `400 response when invalid start date supplied`() {
      mockMvc.delete("/migrate-appointment/MDI?startDate=invalid")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Error converting 'startDate' (invalid): Method parameter 'startDate': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
            }
          }
        }

      verifyNoInteractions(deleteMigratedAppointmentsJob)
    }

    @Test
    fun `400 response when start date is in the past`() {
      val startDate = LocalDate.now().minusDays(1)
      mockMvc.delete("/migrate-appointment/MDI?startDate=$startDate")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Exception: Start date must not be in the past")
            }
          }
        }

      verifyNoInteractions(deleteMigratedAppointmentsJob)
    }
  }

  @Nested
  @DisplayName("Migrate appointments summary")
  inner class MigrateAppointmentsSummaryTests {
    @Nested
    @DisplayName("Authorization tests")
    inner class AuthorizationTests {
      @Test
      @WithMockUser(roles = ["NOMIS_APPOINTMENTS"])
      fun `Migrate appointment summary (ROLE_NOMIS_APPOINTMENTS) - 202`() {
        mockMvcWithSecurity.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now()}&categoryCodes=AC1")
          .andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["MIGRATE_APPOINTMENTS"])
      fun `Migrate appointment summary (ROLE_MIGRATE_APPOINTMENTS) - 202`() {
        mockMvcWithSecurity.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now()}&categoryCodes=AC1")
          .andExpect { status { isOk() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Migrate appointment (ROLE_ACTIVITY_HUB) - 403`() {
        mockMvcWithSecurity.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now()}&categoryCodes=AC1")
          .andExpect { status { isForbidden() } }
      }

      @Test
      @WithMockUser(roles = ["UNKNOWN"])
      fun `Migrate appointment (ROLE_UNKNOWN) - 403`() {
        mockMvcWithSecurity.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now()}&categoryCodes=AC1")
          .andExpect { status { isForbidden() } }
      }
    }

    @Test
    fun `200 response when category code supplied`() {
      val startDate = LocalDate.now()
      mockMvc.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now()}&categoryCodes=AC1")
        .andExpect { status { isOk() } }

      verify(migrateAppointmentService).getAppointmentSummary("MDI", startDate, listOf("AC1"))
    }

    @Test
    fun `400 response when no start date supplied`() {
      mockMvc.get("/migrate-appointment/MDI/summary?categoryCodes=AC1")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Required request parameter 'startDate' for method parameter type LocalDate is not present")
            }
          }
        }

      verifyNoInteractions(migrateAppointmentService)
    }

    @Test
    fun `400 response when invalid start date supplied`() {
      mockMvc.get("/migrate-appointment/MDI/summary?startDate=invalid&categoryCodes=AC1")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Error converting 'startDate' (invalid): Method parameter 'startDate': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
            }
          }
        }

      verifyNoInteractions(migrateAppointmentService)
    }

    @Test
    fun `400 response when start date is in the past`() {
      val startDate = LocalDate.now().minusDays(1)
      mockMvc.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now().minusDays(1)}&categoryCodes=AC1")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Exception: Start date must not be in the past")
            }
          }
        }

      verifyNoInteractions(deleteMigratedAppointmentsJob)
    }

    @Test
    fun `400 response when no category codes are supplied`() {
      mockMvc.get("/migrate-appointment/MDI/summary?startDate=${LocalDate.now()}")
        .andExpect { status { isBadRequest() } }
        .andExpect {
          content {
            jsonPath("$.userMessage") {
              value("Required request parameter 'categoryCodes' for method parameter type List is not present")
            }
          }
        }

      verifyNoInteractions(migrateAppointmentService)
    }
  }
}
