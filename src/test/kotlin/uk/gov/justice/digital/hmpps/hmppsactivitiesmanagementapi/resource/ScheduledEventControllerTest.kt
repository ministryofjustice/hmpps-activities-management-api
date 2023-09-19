package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import java.time.LocalDate

@WebMvcTest(controllers = [ScheduledEventController::class])
@ContextConfiguration(classes = [ScheduledEventController::class])
class ScheduledEventControllerTest : ControllerTestBase<ScheduledEventController>() {

  @MockBean
  private lateinit var scheduledEventService: ScheduledEventService

  @MockBean
  private lateinit var referenceCodeService: ReferenceCodeService

  @MockBean
  private lateinit var locationService: LocationService

  override fun controller() = ScheduledEventController(scheduledEventService, referenceCodeService, locationService)

  @BeforeEach
  fun setupMocks() {
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf("" to appointmentCategoryReferenceCode("")))

    whenever(locationService.getLocationsForAppointmentsMap("MDI"))
      .thenReturn(mapOf(101L to appointmentLocation(101, "MDI")))
  }

  @Test
  fun `getScheduledEventsForMultiplePrisoners - 200 response with events`() {
    val prisonerNumbers = setOf("G1234GG")
    val result = PrisonerScheduledEventsFixture.instance()

    whenever(
      scheduledEventService.getScheduledEventsForMultiplePrisoners(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        locationService.getLocationsForAppointmentsMap("MDI"),
      ),
    ).thenReturn(result)

    val response =
      mockMvc.getScheduledEventsForMultiplePrisoners(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM.name,
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

    verify(scheduledEventService).getScheduledEventsForMultiplePrisoners(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      locationService.getLocationsForAppointmentsMap("MDI"),
    )
  }

  @Test
  fun `getScheduledEventsForMultiplePrisoners - Error response when service throws exception`() {
    val prisonerNumbers = setOf("G1234GG")
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()

    whenever(
      scheduledEventService.getScheduledEventsForMultiplePrisoners(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        locationService.getLocationsForAppointmentsMap("MDI"),
      ),
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getScheduledEventsForMultiplePrisoners(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM.name,
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(scheduledEventService).getScheduledEventsForMultiplePrisoners(
      "MDI",
      prisonerNumbers,
      LocalDate.of(2022, 10, 1),
      TimeSlot.AM,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      locationService.getLocationsForAppointmentsMap("MDI"),
    )
  }

  @Test
  fun `getScheduledEventsForMultiplePrisoners - 400 response when no date provided`() {
    val prisonerNumbers = setOf("G1234GG")

    mockMvc.post("/scheduled-events/prison/MDI") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers,
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForMultiplePrisoners - 400 response when no prisoner numbers are provided`() {
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForMultiplePrisoners - 400 response when date incorrect format`() {
    val prisonerNumbers = setOf("G1234GG")
    mockMvc.post("/scheduled-events/prison/MDI?date=20/12/2022") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers,
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForMultiplePrisoners - 400 response when time slot is an incorrect format`() {
    val prisonerNumbers = setOf("G1234GG")
    mockMvc.post("/scheduled-events/prison/MDI?date=2022-12-01&timeSlot=AF") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers,
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - 200 response with events`() {
    val result = PrisonerScheduledEventsFixture.instance()
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    whenever(
      scheduledEventService.getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        LocalDateRange(startDate, endDate),
        null,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        locationService.getLocationsForAppointmentsMap("MDI"),
      ),
    ).thenReturn(result)

    val response = mockMvc.getScheduledEventsForSinglePrisoner(
      "MDI",
      "G1234GG",
      startDate,
      endDate,
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

    verify(scheduledEventService).getScheduledEventsForSinglePrisoner(
      "MDI",
      "G1234GG",
      LocalDateRange(startDate, endDate),
      null,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      locationService.getLocationsForAppointmentsMap("MDI"),
    )
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    whenever(
      scheduledEventService.getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        LocalDateRange(startDate, endDate),
        null,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        locationService.getLocationsForAppointmentsMap("MDI"),
      ),
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getScheduledEventsForSinglePrisoner(
      "MDI",
      "G1234GG",
      startDate,
      endDate,
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(scheduledEventService).getScheduledEventsForSinglePrisoner(
      "MDI",
      "G1234GG",
      LocalDateRange(startDate, endDate),
      null,
      referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      locationService.getLocationsForAppointmentsMap("MDI"),
    )
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - 200 response when date range equals 3 months`() {
    val result = PrisonerScheduledEventsFixture.instance()
    val startDate = LocalDate.of(2022, 11, 1)
    val endDate = LocalDate.of(2023, 2, 1)

    whenever(
      scheduledEventService.getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        LocalDateRange(startDate, endDate),
        null,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        locationService.getLocationsForAppointmentsMap("MDI"),
      ),
    ).thenReturn(result)

    mockMvc.get("/scheduled-events/prison/MDI") {
      param("prisonerNumber", "G1234GG")
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
  fun `getScheduledEventsForSinglePrisoner - 400 response when end date missing`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
      param("prisonerNumber", "G1234GG")
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - 400 response when start date missing`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
      param("prisonerNumber", "G1234GG")
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - 400 response when start date incorrect format`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
      param("prisonerNumber", "G1234GG")
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - 400 response when end date incorrect format`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
      param("prisonerNumber", "G1234GG")
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
            value("Bad Request")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForSinglePrisoner - 400 response when date range exceeds 3 months`() {
    mockMvc.get("/scheduled-events/prison/MDI") {
      param("prisonerNumber", "G1234GG")
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

  private fun MockMvc.getScheduledEventsForSinglePrisoner(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate,
  ) = get("/scheduled-events/prison/$prisonCode?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")

  private fun MockMvc.getScheduledEventsForMultiplePrisoners(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: String,
  ) = post("/scheduled-events/prison/$prisonCode?date=$date&timeSlot=$timeSlot") {
    accept = MediaType.APPLICATION_JSON
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      prisonerNumbers,
    )
  }
}
