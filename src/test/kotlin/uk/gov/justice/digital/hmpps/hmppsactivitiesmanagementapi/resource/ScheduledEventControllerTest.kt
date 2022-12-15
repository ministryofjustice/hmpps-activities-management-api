package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
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
  fun `getScheduledEventsForOffenderList - 200 response when get prisoner scheduled events found`() {
    val prisonerNumbers = setOf("G4793VF")
    val result = PrisonerScheduledEventsFixture.instance()
    whenever(
      scheduledEventService.getScheduledEventsForOffenderList(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM
      )
    ).thenReturn(result)

    val response =
      mockMvc.getScheduledEventsForOffenderList("MDI", prisonerNumbers, LocalDate.of(2022, 10, 1), TimeSlot.AM.name)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))
    verify(scheduledEventService).getScheduledEventsForOffenderList(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM
    )
  }

  @Test
  fun `getScheduledEventsForOffenderList - Error response when service throws exception`() {
    val prisonerNumbers = setOf("G4793VF")
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      scheduledEventService.getScheduledEventsForOffenderList(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM
      )
    ).thenThrow(RuntimeException("Error"))

    val response =
      mockMvc.getScheduledEventsForOffenderList("MDI", prisonerNumbers, LocalDate.of(2022, 10, 1), TimeSlot.AM.name)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { is5xxServerError() } }
        .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)
    verify(scheduledEventService).getScheduledEventsForOffenderList(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM
    )
  }

  @Test
  fun `getScheduledEventsForOffenderList - 200 response when no date or timeslot`() {

    val prisonerNumbers = setOf("G4793VF")
    val result = PrisonerScheduledEventsFixture.instance()
    whenever(
      scheduledEventService.getScheduledEventsForOffenderList("MDI", prisonerNumbers, null, null)
    ).thenReturn(result)

    mockMvc.post("/prisons/MDI/scheduled-events") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers
      )
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
  fun `getScheduledEventsForOffenderList - 400 response when prisoner numbers are missing`() {
    mockMvc.post("/prisons/MDI/scheduled-events") {
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
            value("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ScheduledEventController.getScheduledEventsForOffenderList(java.lang.String,java.time.LocalDate,uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot,java.util.Set<java.lang.String>)")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForOffenderList - 400 response when date incorrect format`() {
    val prisonerNumbers = setOf("G4793VF")
    mockMvc.post("/prisons/MDI/scheduled-events?date=20/12/2022") {
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
  fun `getScheduledEventsForOffenderList - 400 response when timeslot incorrect format`() {
    val prisonerNumbers = setOf("G4793VF")
    mockMvc.post("/prisons/MDI/scheduled-events?timeSlot=AF") {
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
  fun `getScheduledEventsByDateRange - 200 response when get prisoner scheduled events found`() {
    val result = PrisonerScheduledEventsFixture.instance()
    whenever(
      scheduledEventService.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    ).thenReturn(result)

    val response = mockMvc.getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDate.of(2022, 10, 1),
      LocalDate.of(2022, 11, 5)
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

    verify(scheduledEventService).getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )
  }

  @Test
  fun `getScheduledEventsByDateRange - Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      scheduledEventService.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDate.of(2022, 10, 1),
      LocalDate.of(2022, 11, 5)
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(scheduledEventService).getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )
  }

  @Test
  fun `getScheduledEventsByDateRange - 200 response when date range equals 3 months`() {

    val result = PrisonerScheduledEventsFixture.instance()
    whenever(
      scheduledEventService.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 11, 1), LocalDate.of(2023, 2, 1))
      )
    ).thenReturn(result)

    mockMvc.get("/prisons/MDI/scheduled-events") {
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
  fun `getScheduledEventsByDateRange - 400 response when end date missing`() {
    mockMvc.get("/prisons/MDI/scheduled-events") {
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
  fun `getScheduledEventsByDateRange - 400 response when start date missing`() {
    mockMvc.get("/prisons/MDI/scheduled-events") {
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
  fun `getScheduledEventsByDateRange - 400 response when start date incorrect format`() {
    mockMvc.get("/prisons/MDI/scheduled-events") {
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
  fun `getScheduledEventsByDateRange - 400 response when end date incorrect format`() {
    mockMvc.get("/prisons/MDI/scheduled-events") {
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
  fun `getScheduledEventsByDateRange - 400 response when date range exceeds 3 months`() {
    mockMvc.get("/prisons/MDI/scheduled-events") {
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

  private fun MockMvc.getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get("/prisons/$prisonCode/scheduled-events?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")

  private fun MockMvc.getScheduledEventsForOffenderList(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: String
  ) =
    post("/prisons/$prisonCode/scheduled-events?date=$date&timeSlot=$timeSlot") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers
      )
    }
}
