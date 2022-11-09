package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformActivityScheduleInstances
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [ActivityScheduleInstanceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ActivityScheduleInstanceController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class ActivityScheduleInstanceControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var scheduledInstanceService: ScheduledInstanceService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(ActivityScheduleInstanceController(scheduledInstanceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `200 response when get prisoner scheduled instances found`() {
    val results = transformActivityScheduleInstances(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

    whenever(
      scheduledInstanceService.getActivityScheduleInstancesByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    ).thenReturn(results)

    val response = mockMvc.getPrisonerScheduledInstances(
      "MDI", "A11111A",
      LocalDate.of(2022, 10, 1),
      LocalDate.of(2022, 11, 5)
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(results))

    verify(scheduledInstanceService).getActivityScheduleInstancesByDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )
  }

  @Test
  fun `400 response when end date missing`() {
    mockMvc.get("/prisons/MDI/scheduled-instances") {
      param("prisonerNumber", "A11111A")
      param("startDate", "2022-10-01")
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Required request parameter 'endDate' for method parameter type LocalDate is not present")
          }
        }
      }
  }

  @Test
  fun `400 response when start date missing`() {
    mockMvc.get("/prisons/MDI/scheduled-instances") {
      param("prisonerNumber", "A11111A")
      param("endDate", "2022-10-01")
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Required request parameter 'startDate' for method parameter type LocalDate is not present")
          }
        }
      }
  }

  @Test
  fun `400 response when start date incorrect format`() {
    mockMvc.get("/prisons/MDI/scheduled-instances") {
      param("prisonerNumber", "A11111A")
      param("startDate", "01/10/2022")
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Error converting 'startDate' (01/10/2022): Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }
  }

  @Test
  fun `400 response when end date incorrect format`() {
    mockMvc.get("/prisons/MDI/scheduled-instances") {
      param("prisonerNumber", "A11111A")
      param("startDate", "2022-10-01")
      param("endDate", "01/10/2022")
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Error converting 'endDate' (01/10/2022): Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }
  }

  @Test
  fun `400 response when date range exceeds 3 moths`() {
    mockMvc.get("/prisons/MDI/scheduled-instances") {
      param("prisonerNumber", "A11111A")
      param("startDate", "2022-11-01")
      param("endDate", "2023-02-02")
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Validation failure: Date range cannot exceed 3 months")
          }
        }
      }
  }

  @Test
  fun `200 response when date range equals 3 moths`() {
    mockMvc.get("/prisons/MDI/scheduled-instances") {
      param("prisonerNumber", "A11111A")
      param("startDate", "2022-11-01")
      param("endDate", "2023-02-01")
    }
      .andDo { print() }
      .andExpect {
        status {
          isOk()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
        }
      }
  }

  private fun MockMvc.getPrisonerScheduledInstances(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get("/prisons/$prisonCode/scheduled-instances?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
}
