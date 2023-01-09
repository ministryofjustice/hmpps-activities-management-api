package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import java.time.LocalDate

@WebMvcTest(controllers = [ScheduledEventController::class])
@ContextConfiguration(classes = [ScheduledEventController::class])
class ScheduledEventControllerTest : ControllerTestBase<ScheduledEventController>() {

  @MockBean
  private lateinit var scheduledEventService: ScheduledEventService

  override fun controller() = ScheduledEventController(scheduledEventService)

  @Test
  fun `getScheduledEventsByPrisonAndPrisonersAndDateRange - 200 response with events`() {
    val prisonerNumbers = setOf("G4793VF")
    val result = PrisonerScheduledEventsFixture.instance()
    whenever(
      scheduledEventService.getScheduledEventsByPrisonAndPrisonersAndDateRange(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM
      )
    ).thenReturn(result)

    val response =
      mockMvc.getScheduledEventsByPrisonAndPrisonersAndDateRange(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM.name
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

    verify(scheduledEventService).getScheduledEventsByPrisonAndPrisonersAndDateRange(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM
    )
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonersAndDateRange - Error response when service throws exception`() {
    val prisonerNumbers = setOf("G4793VF")
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      scheduledEventService.getScheduledEventsByPrisonAndPrisonersAndDateRange(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM
      )
    ).thenThrow(RuntimeException("Error"))

    val response =
      mockMvc.getScheduledEventsByPrisonAndPrisonersAndDateRange(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM.name
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { is5xxServerError() } }
        .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(scheduledEventService).getScheduledEventsByPrisonAndPrisonersAndDateRange(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM
    )
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonersAndDateRange - 400 response when no date provided`() {
    val prisonerNumbers = setOf("G4793VF")

    mockMvc.post("/scheduled-events/prison/MDI") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers
      )
    }
      .andDo { print() }
      .andExpect {
        status {
          isBadRequest()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonersAndDateRange - 400 response when no prisoner numbers are provided`() {
    mockMvc.post("/scheduled-events/prison/MDI?date=2022-12-14") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value(StringStartsWith("Required request body is missing:"))
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonersAndDateRange - 400 response when date incorrect format`() {
    val prisonerNumbers = setOf("G4793VF")
    mockMvc.post("/scheduled-events/prison/MDI?date=20/12/2022") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers
      )
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Error converting 'date' (20/12/2022): Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonersAndDateRange - 400 response when time slot is an incorrect format`() {
    val prisonerNumbers = setOf("G4793VF")
    mockMvc.post("/scheduled-events/prison/MDI?date=2022-12-01&timeSlot=AF") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers
      )
    }
      .andDo { print() }
      .andExpect {
        status {
          is4xxClientError()
        }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Error converting 'timeSlot' (AF): Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 200 response with events`() {
    val result = PrisonerScheduledEventsFixture.instance()
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    whenever(
      scheduledEventService.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        "MDI",
        "A11111A",
        LocalDateRange(startDate, endDate),
      )
    ).thenReturn(result)

    val response = mockMvc.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      "MDI",
      "A11111A",
      startDate,
      endDate,
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

    verify(scheduledEventService).getScheduledEventsByPrisonAndPrisonerAndDateRange(
      "MDI",
      "A11111A",
      LocalDateRange(startDate, endDate),
    )
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    whenever(
      scheduledEventService.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        "MDI",
        "A11111A",
        LocalDateRange(startDate, endDate),
      )
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      "MDI",
      "A11111A",
      startDate,
      endDate,
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(scheduledEventService).getScheduledEventsByPrisonAndPrisonerAndDateRange(
      "MDI",
      "A11111A",
      LocalDateRange(startDate, endDate),
    )
  }

  @Test
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 200 response when date range equals 3 months`() {
    val result = PrisonerScheduledEventsFixture.instance()
    val startDate = LocalDate.of(2022, 11, 1)
    val endDate = LocalDate.of(2023, 2, 1)

    whenever(
      scheduledEventService.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        "MDI",
        "A11111A",
        LocalDateRange(startDate, endDate),
      )
    ).thenReturn(result)

    mockMvc.get("/scheduled-events/prison/MDI") {
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

  @Test
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 400 response when end date missing`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
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
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 400 response when start date missing`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
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
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 400 response when start date incorrect format`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
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
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 400 response when end date incorrect format`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
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
  fun `getScheduledEventsByPrisonAndPrisonerAndDateRange - 400 response when date range exceeds 3 months`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
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

  private fun MockMvc.getScheduledEventsByPrisonAndPrisonerAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get("/scheduled-events/prison/$prisonCode?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")

  private fun MockMvc.getScheduledEventsByPrisonAndPrisonersAndDateRange(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: String
  ) =
    post("/scheduled-events/prison/$prisonCode?date=$date&timeSlot=$timeSlot") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers
      )
    }
}
