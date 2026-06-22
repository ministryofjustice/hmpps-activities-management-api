package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentCategoryService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InternalLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import java.time.LocalDate
import java.util.*

@WebMvcTest(controllers = [ScheduledEventController::class])
@ContextConfiguration(classes = [ScheduledEventController::class])
class ScheduledEventControllerTest : ControllerTestBase() {

  @MockitoBean
  private lateinit var scheduledEventService: ScheduledEventService

  @MockitoBean
  private lateinit var appointmentCategoryService: AppointmentCategoryService

  @MockitoBean
  private lateinit var internalLocationService: InternalLocationService

  @BeforeEach
  fun setupMocks() {
    whenever(appointmentCategoryService.getAll())
      .thenReturn(mapOf("" to appointmentCategory("")))
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
        appointmentCategoryService.getAll(),
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
      appointmentCategoryService.getAll(),
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
        appointmentCategoryService.getAll(),
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
      appointmentCategoryService.getAll(),
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
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
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
            value(StringStartsWith("Required request body is missing:"))
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
            value("Error converting 'date' (20/12/2022): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
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
            value("Error converting 'timeSlot' (AF): Method parameter 'timeSlot': Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
          }
        }
      }
  }

  @Test
  fun `getScheduledEventsForMultipleLocations - 200 response when internal locations with events found`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val timeSlot = TimeSlot.AM
    val locations = setOf(internalLocationEvents())
    val internalLocationIds = locations.map { it.id }.toSet()

    whenever(internalLocationService.getInternalLocationEvents(prisonCode, internalLocationIds, date, timeSlot)).thenReturn(locations)

    val response = mockMvc.getInternalLocationEvents(prisonCode, internalLocationIds, date, timeSlot)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(locations))
  }

  @Test
  fun `getScheduledEventsForMultipleLocations - 200 response when no time slot supplied`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val locations = setOf(internalLocationEvents())
    val internalLocationIds = locations.map { it.id }.toSet()

    whenever(internalLocationService.getInternalLocationEvents(prisonCode, internalLocationIds, date, null)).thenReturn(locations)

    val response = mockMvc.getInternalLocationEvents(prisonCode, internalLocationIds, date, null)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(locations))
  }

  @Test
  fun `getScheduledEventsForMultipleLocations - 400 response when no date supplied`() {
    mockMvc.post("/scheduled-events/prison/MDI/locations") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        setOf(1),
      )
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `getScheduledEventsForMultipleLocations - 400 response when invalid date supplied`() {
    mockMvc.post("/scheduled-events/prison/MDI/locations?date=invalid") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        setOf(1),
      )
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'date' (invalid): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `getScheduledEventsForMultipleLocations - 400 response when invalid time slot supplied`() {
    val date = LocalDate.now()
    mockMvc.post("/scheduled-events/prison/MDI/locations?date=$date&timeSlot=no") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        setOf(1),
      )
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'timeSlot' (no): Method parameter 'timeSlot': Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `getScheduledEventsForMultipleLocations - 500 response when service throws exception`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val internalLocationIds = setOf(1L)

    whenever(internalLocationService.getInternalLocationEvents(prisonCode, internalLocationIds, date, null)).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getInternalLocationEvents(prisonCode, internalLocationIds, date, null)
      .andExpect { status { isInternalServerError() } }
      .andReturn().response

    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    assertThat(response.contentAsString + "\n").isEqualTo(result)
  }

  @Test
  fun `getScheduledEventsByDPSLocationId - 200 response when internal location with events found`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val timeSlot = TimeSlot.AM
    val location = internalLocationEvents()

    whenever(internalLocationService.getLocationEvents(prisonCode, location.dpsLocationId, date, timeSlot)).thenReturn(location)

    val response = mockMvc.getScheduledEventsByDPSLocationId(prisonCode, location.dpsLocationId, date, timeSlot)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(location))
  }

  @Test
  fun `getScheduledEventsByDPSLocationId - 200 response when no time slot supplied`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val location = internalLocationEvents()

    whenever(internalLocationService.getLocationEvents(prisonCode, location.dpsLocationId, date, null)).thenReturn(location)

    val response = mockMvc.getScheduledEventsByDPSLocationId(prisonCode, location.dpsLocationId, date, null)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(location))
  }

  @Test
  fun `getScheduledEventsByDPSLocationId - 400 response when no date supplied`() {
    mockMvc.get("/scheduled-events/prison/MDI/location-events?dpsLocationId=${UUID.randomUUID()}") { accept = MediaType.APPLICATION_JSON }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `getScheduledEventsByDPSLocationId - 400 response when invalid date supplied`() {
    mockMvc.get("/scheduled-events/prison/MDI/location-events?dpsLocationId=${UUID.randomUUID()}&date=invalid") {
      accept = MediaType.APPLICATION_JSON
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'date' (invalid): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `getScheduledEventsByDPSLocationId - 400 response when invalid time slot supplied`() {
    mockMvc.get("/scheduled-events/prison/MDI/location-events?dpsLocationId=${UUID.randomUUID()}&date=${LocalDate.now()}&timeSlot=no") {
      accept = MediaType.APPLICATION_JSON
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'timeSlot' (no): Method parameter 'timeSlot': Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `getScheduledEventsByDPSLocationId - 500 response when service throws exception`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val dpsLocationId = UUID.randomUUID()

    whenever(internalLocationService.getLocationEvents(prisonCode, dpsLocationId, date, null)).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getScheduledEventsByDPSLocationId(prisonCode, dpsLocationId, date, null)
      .andExpect { status { isInternalServerError() } }
      .andReturn().response

    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    assertThat(response.contentAsString + "\n").isEqualTo(result)
  }

  @Test
  fun `getExternalMovementsForMovementList - 200 response when no external movements found`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val result = emptySet<LocationEvents>()

    whenever(scheduledEventService.getExternalMovements(prisonCode, date, null))
      .thenReturn(result)

    val response = mockMvc.getExternalMovements(prisonCode, date, null)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))
  }

  @Test
  fun `getExternalMovementsForMovementList - 200 response with time slot`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()

    whenever(scheduledEventService.getExternalMovements(prisonCode, date, TimeSlot.AM))
      .thenReturn(emptySet())

    mockMvc.getExternalMovements(prisonCode, date, TimeSlot.AM)
      .andExpect { status { isOk() } }

    verify(scheduledEventService).getExternalMovements(prisonCode, date, TimeSlot.AM)
  }

  @Test
  fun `getExternalMovementsForMovementList - 400 response when no date supplied`() {
    mockMvc.get("/scheduled-events/prison/MDI/external-movements") {
      accept = MediaType.APPLICATION_JSON
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
          }
        }
      }

    verifyNoInteractions(scheduledEventService)
  }

  @Test
  fun `getExternalMovementsForMovementList - 400 response when invalid date supplied`() {
    mockMvc.get("/scheduled-events/prison/MDI/external-movements?date=invalid") {
      accept = MediaType.APPLICATION_JSON
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'date' (invalid): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }

    verifyNoInteractions(scheduledEventService)
  }

  @Test
  fun `getExternalMovementsForMovementList - 400 response when invalid time slot supplied`() {
    val date = LocalDate.now()
    mockMvc.get("/scheduled-events/prison/MDI/external-movements?date=$date&timeSlot=no") {
      accept = MediaType.APPLICATION_JSON
    }
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'timeSlot' (no): Method parameter 'timeSlot': Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
          }
        }
      }

    verifyNoInteractions(scheduledEventService)
  }

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

  private fun MockMvc.getInternalLocationEvents(
    prisonCode: String,
    internalLocationIds: Set<Long>,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ) = post("/scheduled-events/prison/$prisonCode/locations?date=$date" + (timeSlot?.let { "&timeSlot=$timeSlot" } ?: "")) {
    accept = MediaType.APPLICATION_JSON
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      internalLocationIds,
    )
  }.andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }

  private fun MockMvc.getScheduledEventsByDPSLocationId(
    prisonCode: String,
    dpsLocationId: UUID,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ) = get("/scheduled-events/prison/$prisonCode/location-events?dpsLocationId=$dpsLocationId&date=$date" + (timeSlot?.let { "&timeSlot=$timeSlot" } ?: "")) {
    accept = MediaType.APPLICATION_JSON
  }.andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }

  private fun MockMvc.getExternalMovements(
    prisonCode: String,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ) = get("/scheduled-events/prison/$prisonCode/external-movements?date=$date" + (timeSlot?.let { "&timeSlot=$timeSlot" } ?: "")) {
    accept = MediaType.APPLICATION_JSON
  }.andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
}
