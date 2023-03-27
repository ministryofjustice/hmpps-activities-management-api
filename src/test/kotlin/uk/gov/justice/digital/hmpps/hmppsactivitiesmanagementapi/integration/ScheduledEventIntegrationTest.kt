package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

class ScheduledEventIntegrationTest : IntegrationTestBase() {

  @Nested
  inner class GetByPrisonerAndDateRange {

    private fun WebTestClient.getByPrisonerAndDateRange(prisonCode: String, prisonerNumber: String, startDate: LocalDate, endDate: LocalDate) =
      get()
        .uri("/scheduled-events/prison/$prisonCode?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerScheduledEvents::class.java)
        .returnResult().responseBody

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-active.sql")
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `GET - prison rolled-out - 200 success with activities from the DB`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A11111A"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 1)

      // Setup prison API stubs
      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))

      val scheduledEvents = webTestClient.getByPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(1)
        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(3)
        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(4)
        assertThat(appointments).hasSize(1)
        assertThat(appointments!![0].priority).isEqualTo(5)
        assertThat(activities).hasSize(6)
        assertThat(activities!![0].priority).isEqualTo(6)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `GET - prison not rolled-out - 200 success with activities from prison API`() {
      val prisonCode = "MDI"
      val prisonerNumber = "G4793VF"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Set up prison API stubs
      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))

      val scheduledEvents = webTestClient.getByPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(1)
        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(3)
        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(4)
        assertThat(appointments).hasSize(1)
        assertThat(appointments!![0].priority).isEqualTo(5)
        assertThat(activities).hasSize(2)
        assertThat(activities!![0].priority).isEqualTo(6)
      }
    }

    @Test
    fun `GET - prison not rolled-out - 404 if prisoner details not found`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Set up prison API stubs
      prisonerSearchApiMockServer.stubSearchByPrisonerNumberNotFound(prisonerNumber)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)

      val errorResponse = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/scheduled-events/prison/$prisonCode")
            .queryParam("prisonerNumber", prisonerNumber)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
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
        assertThat(developerMessage).isEqualTo("Prisoner '$prisonerNumber' not found")
        assertThat(moreInfo).isNull()
        assertThat(status).isEqualTo(404)
        assertThat(userMessage).isEqualTo("Not found: Prisoner '$prisonerNumber' not found")
      }
    }

    @Test
    fun `GET - prison not rolled-out - 404 bookingId does not exist for appointments`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointmentsNotFound(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)

      val errorResponse = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/scheduled-events/prison/$prisonCode")
            .queryParam("prisonerNumber", prisonerNumber)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
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
    fun `GET - prison not rolled-out - 404 when bookingId does not exist for activities`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledActivitiesNotFound(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)

      val errorResponse = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/scheduled-events/prison/$prisonCode")
            .queryParam("prisonerNumber", prisonerNumber)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
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
    fun `GET - prison not rolled-out - 404 when bookingId does not exist for visits`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisitsNotFound(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)

      val errorResponse = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/scheduled-events/prison/$prisonCode")
            .queryParam("prisonerNumber", prisonerNumber)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
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
    fun `GET - prison not rolled-out - 404 when bookingId does not exist for court hearings`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearingsNotFound(bookingId, startDate, endDate)

      val errorResponse = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/scheduled-events/prison/$prisonCode")
            .queryParam("prisonerNumber", prisonerNumber)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
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
  }

  @Nested
  inner class PostByPrisonersAndDate {

    private fun WebTestClient.postByPrisonersAndDate(
      prisonCode: String,
      prisonerNumbers: Set<String>,
      date: LocalDate,
    ) =
      post()
        .uri("/scheduled-events/prison/$prisonCode?date=$date")
        .bodyValue(prisonerNumbers)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerScheduledEvents::class.java)
        .returnResult().responseBody

    private fun readAppointmentsStubbedResults() = mapper.readValue(
      this::class.java.getResource("/__files/scheduled-events/appointments-1.json"),
      object : TypeReference<List<ScheduledEvent>>() {},
    )

    private fun readCourtHearingStubbedResults() = mapper.readValue(
      this::class.java.getResource("/__files/scheduled-events/court-hearings-1.json"),
      object : TypeReference<List<ScheduledEvent>>() {},
    )

    private fun readVisitsStubbedResults() = mapper.readValue(
      this::class.java.getResource("/__files/scheduled-events/visits-1.json"),
      object : TypeReference<List<ScheduledEvent>>() {},
    )

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-active.sql")
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `POST - prison rolled-out - 200 success with activities from the DB`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A11111A", "A22222A")
      val date = LocalDate.of(2022, 10, 1)

      // Ignores query parameters - matches on url path only
      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers)

      val scheduledEvents = webTestClient.postByPrisonersAndDate(prisonCode, prisonerNumbers.toSet(), date)

      // Get the prison API stubbed data to compare results against
      val appointmentsResults = readAppointmentsStubbedResults()
      val courtHearingsResults = readCourtHearingStubbedResults()
      val visitsResults = readVisitsStubbedResults()

      // Compare the results from the endpoint with the stubs provided & database activities
      with(scheduledEvents!!) {
        assertThat(prisonerNumbers).contains("A11111A")
        assertThat(appointments).isEqualTo(appointmentsResults)
        assertThat(courtHearings).isEqualTo(courtHearingsResults)
        assertThat(visits).isEqualTo(visitsResults)
        assertThat(activities).hasSize(1)
        assertThat(adjudications).hasSize(2)
        with(activities!![0]) {
          assertThat(prisonerNumber).isEqualTo("A11111A")
          assertThat(eventType).isEqualTo("PRISON_ACT")
          assertThat(event).isEqualTo("Geography")
          assertThat(eventDesc).isEqualTo("Geography AM")
          assertThat(details).isEqualTo("Geography: Geography AM")
          assertThat(priority).isEqualTo(6)
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `POST - prison not rolled-out - 200 success with activities from prison API`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G4793VF", "A5193DY")
      val date = LocalDate.of(2022, 10, 1)

      // Ignores query parameters - matches on url path only
      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledActivitiesForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers)

      val scheduledEvents = webTestClient.postByPrisonersAndDate(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(appointments).hasSize(2)
        assertThat(activities).hasSize(2)
        assertThat(adjudications).hasSize(2)
      }
    }
  }
}
