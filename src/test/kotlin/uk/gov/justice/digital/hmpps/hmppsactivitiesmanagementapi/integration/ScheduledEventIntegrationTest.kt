package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

class ScheduledEventIntegrationTest : IntegrationTestBase() {

  @Test
  fun `getScheduledEventsForOffenderList - returns all 10 rows that satisfy the criteria`() {

    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF", "A5193DY")
    val date = LocalDate.of(2022, 10, 1)

    prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
    prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
    prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)

    val scheduledEvents =
      webTestClient.getScheduledEventsForOffenderList(
        "MDI",
        prisonerNumbers,
        date,
      )

    val appointmentsResults: List<ScheduledEvent> = mapper.readValue(
      this::class.java.getResource("/__files/scheduled-events/appointments-1.json"),
      object : TypeReference<List<ScheduledEvent>>() {}
    )

    val courtHearingsResults: List<ScheduledEvent> = mapper.readValue(
      this::class.java.getResource("/__files/scheduled-events/court-hearings-1.json"),
      object : TypeReference<List<ScheduledEvent>>() {}
    )

    val visitsResults: List<ScheduledEvent> = mapper.readValue(
      this::class.java.getResource("/__files/scheduled-events/visits-1.json"),
      object : TypeReference<List<ScheduledEvent>>() {}
    )

    with(scheduledEvents!!) {
      assertThat(prisonerNumbers).contains("G4793VF")
      assertThat(appointments).isEqualTo(appointmentsResults)
      assertThat(courtHearings).isEqualTo(courtHearingsResults)
      assertThat(visits).isEqualTo(visitsResults)
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (not rolled out) - returns all 10 rows that satisfy the criteria`() {

    val prisonerNumber = "G4793VF"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    prisonApiMockServer.stubGetScheduledActivities(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledVisits(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val scheduledEvents =
      webTestClient.getScheduledEventsByDateRange(
        "MDI",
        prisonerNumber,
        dateRange.start,
        dateRange.endInclusive
      )

    with(scheduledEvents!!) {
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].priority).isEqualTo(4)
      assertThat(activities).hasSize(2)
      assertThat(activities!![0].priority).isEqualTo(5)
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].priority).isEqualTo(2)
      assertThat(courtHearings).hasSize(4)
      assertThat(courtHearings!![0].priority).isEqualTo(1)
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - 404 if prisoner details not found`() {

    val prisonerNumber = "AAAAA"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetailsNotFound(prisonerNumber)
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledActivities(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledVisits(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/MDI/scheduled-events")
          .queryParam("prisonerNumber", prisonerNumber)
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(prisonerNumber)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Resource with id [AAAAA] not found.")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Resource with id [AAAAA] not found.")
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - 404 if booking id doesnt exist for scheduled appointments`() {

    val prisonerNumber = "AAAAA"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    prisonApiMockServer.stubGetScheduledAppointmentsNotFound(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledActivities(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledVisits(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/MDI/scheduled-events")
          .queryParam("prisonerNumber", prisonerNumber)
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(prisonerNumber)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Offender booking with id 12009930 not found.")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Offender booking with id 12009930 not found.")
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - 404 if booking id doesnt exist for scheduled activities`() {

    val prisonerNumber = "AAAAA"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledActivitiesNotFound(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledVisits(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/MDI/scheduled-events")
          .queryParam("prisonerNumber", prisonerNumber)
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(prisonerNumber)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Offender booking with id 12009930 not found.")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Offender booking with id 12009930 not found.")
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - 404 if booking id doesnt exist for visits`() {

    val prisonerNumber = "AAAAA"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledActivities(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledVisitsNotFound(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/MDI/scheduled-events")
          .queryParam("prisonerNumber", prisonerNumber)
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(prisonerNumber)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Offender booking with id 12009930 not found.")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Offender booking with id 12009930 not found.")
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - 404 if booking id doesnt exist for court hearings`() {

    val prisonerNumber = "AAAAA"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledActivities(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetScheduledVisits(bookingId, dateRange.start, dateRange.endInclusive)
    prisonApiMockServer.stubGetCourtHearingsNotFound(bookingId, dateRange.start, dateRange.endInclusive)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/MDI/scheduled-events")
          .queryParam("prisonerNumber", prisonerNumber)
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(prisonerNumber)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Offender booking with id 12009930 not found.")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Offender booking with id 12009930 not found.")
    }
  }

  private fun WebTestClient.getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get()
      .uri("/prisons/$prisonCode/scheduled-events?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerScheduledEvents::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getScheduledEventsForOffenderList(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate
  ) =
    post()
      .uri("/prisons/$prisonCode/scheduled-events?date=$date")
      .bodyValue(prisonerNumbers)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerScheduledEvents::class.java)
      .returnResult().responseBody
}
