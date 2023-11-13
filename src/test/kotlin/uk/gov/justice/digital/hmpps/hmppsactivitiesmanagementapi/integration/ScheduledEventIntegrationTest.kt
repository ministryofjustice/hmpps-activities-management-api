package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture
import java.time.LocalDate
import java.time.LocalTime

class ScheduledEventIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun setupAppointmentStubs() {
    // Stubs used to find category and location descriptions for appointments
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForTypeUnrestricted(
      "MDI",
      "APP",
      "prisonapi/locations-MDI-appointments.json",
    )
  }

  @Nested
  inner class GetScheduledEventsForSinglePrisoner {
    private fun WebTestClient.getScheduledEventsForSinglePrisoner(prisonCode: String, prisonerNumber: String, startDate: LocalDate, endDate: LocalDate) =
      get()
        .uri("/scheduled-events/prison/$prisonCode?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerScheduledEvents::class.java)
        .returnResult().responseBody

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-appointments.sql")
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `GET single prisoner - activities active, appointments not active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A11111A"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(EventType.VISIT.defaultPriority)
        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        assertThat(appointments).hasSize(1)
        assertThat(appointments!![0].priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        assertThat(activities).hasSize(6)
        assertThat(activities!![0].priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `GET single prisoner - both activities and appointments active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A11111A"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)

        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(EventType.VISIT.defaultPriority)

        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)

        assertThat(appointments).hasSize(1)
        appointments!!.map {
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.appointmentSeriesId).isEqualTo(3)
          assertThat(it.appointmentId).isEqualTo(4)
          assertThat(it.appointmentAttendeeId).isEqualTo(5)
          assertThat(it.internalLocationId).isEqualTo(123)
          assertThat(it.internalLocationCode).isEqualTo("No information available")
          assertThat(it.internalLocationDescription).isEqualTo("No information available")
          assertThat(it.categoryCode).isEqualTo("AC1")
          assertThat(it.categoryDescription).isEqualTo("Appointment Category 1")
          assertThat(it.summary).isEqualTo("Appointment description (Appointment Category 1)")
          assertThat(it.comments).isEqualTo("Appointment level comment")
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 10, 1))
          assertThat(it.startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(10, 30))
        }

        assertThat(activities).hasSize(6)
        assertThat(activities!![0].priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-for-with-exclusions.sql")
    fun `GET single prisoner - scheduled events with exclusions are not returned`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A5193DY"
      val bookingId = 1200993L
      val startDate = LocalDate.now()
      val endDate = LocalDate.now()

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(LocalDate.now())
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(15, 0))
      }
    }

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `GET single prisoner - neither activities and appointments active (both prison API) - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumber = "G4793VF"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)

        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(EventType.VISIT.defaultPriority)

        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)

        assertThat(appointments).hasSize(1)
        appointments!!.map {
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(activities).hasSize(2)
        activities!!.map {
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }
      }
    }

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    fun `GET single prisoner - neither activities nor appointments active - 404 prisoner details not found`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Error stub - prisoner number not found
      prisonerSearchApiMockServer.stubSearchByPrisonerNumberNotFound(prisonerNumber)

      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // No transfers - not today

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
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    fun `GET single prisoner - neither activities or appointments active - 404 bookingId appointments`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Appointments not found stub
      prisonApiMockServer.stubGetScheduledAppointmentsNotFound(bookingId, startDate, endDate)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // No transfers - not today

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
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    fun `GET single prisoner - neither activities or appointments active - 404 bookingId activities`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Activities not found stub
      prisonApiMockServer.stubGetScheduledActivitiesNotFound(bookingId, startDate, endDate)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // Not transfers - not today

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
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    fun `GET single prisoner - neither activities nor appointments rolled out - 404 bookingId visits`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Visits not found stub
      prisonApiMockServer.stubGetScheduledVisitsNotFound(bookingId, startDate, endDate)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // Not transfers - not today

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
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    fun `GET single prisoner - neither activities not appointments active - 404 bookingId court hearings`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Court hearings not found stub
      prisonApiMockServer.stubGetCourtHearingsNotFound(bookingId, startDate, endDate)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, startDate.rangeTo(endDate), listOf(prisonerNumber))
      // Not transfers - not today

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
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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
  inner class GetScheduledEventsForMultiplePrisoners {

    private fun WebTestClient.getScheduledEventsForMultiplePrisoners(
      prisonCode: String,
      prisonerNumbers: Set<String>,
      date: LocalDate,
    ) =
      post()
        .uri("/scheduled-events/prison/$prisonCode?date=$date")
        .bodyValue(prisonerNumbers)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerScheduledEvents::class.java)
        .returnResult().responseBody

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-appointments.sql")
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `POST - multiple prisoners - activities active, appointments not active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A11111A", "A22222A")
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(prisonerNumbers).contains("A11111A")
        assertThat(appointments).hasSize(2)
        assertThat(courtHearings).hasSize(2)
        assertThat(visits).hasSize(2)
        assertThat(activities).hasSize(1)
        activities!!.map {
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }
        assertThat(adjudications).hasSize(2)
      }
    }

    @Test
    @Sql("classpath:test_data/make-MDI-rollout-inactive-for-both.sql")
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `POST - multiple prisoners - neither activities nor appointments active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G4793VF", "A5193DY")
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledActivitiesForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(appointments).hasSize(2)
        appointments!!.map {
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(activities).hasSize(2)
        activities!!.map {
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(visits).hasSize(2)
        visits!!.map {
          assertThat(it.eventType).isEqualTo(EventType.VISIT.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.VISIT.defaultPriority)
        }

        assertThat(courtHearings).hasSize(2)
        courtHearings!!.map {
          assertThat(it.eventType).isEqualTo(EventType.COURT_HEARING.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        }

        assertThat(adjudications).hasSize(2)
        adjudications!!.map {
          assertThat(it.eventType).isEqualTo(EventType.ADJUDICATION_HEARING.name)
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-for-events.sql")
    @Sql("classpath:test_data/seed-appointment-group-id-4.sql")
    fun `POST - multiple prisoners - activities and appointments active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G4793VF", "A5193DY")
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(appointments).hasSize(2)
        appointments!!.map {
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }
        assertThat(activities).hasSize(2)
        activities!!.map {
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }
        assertThat(courtHearings).hasSize(2)
        assertThat(visits).hasSize(2)
        assertThat(adjudications).hasSize(2)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-for-with-exclusions.sql")
    fun `POST - multiple prisoners - scheduled events with exclusions are not returned`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A5193DY")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubAdjudicationHearing(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(LocalDate.now())
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(15, 0))
      }
    }
  }

  @Nested
  @DisplayName("getInternalLocationEvents")
  inner class GetInternalLocationEvents {
    private val prisonCode = "MDI"

    private val activityLocation1 = internalLocation(1L, prisonCode = prisonCode, description = "MDI-ACT-LOC1", userDescription = "Activity Location 1")
    private val activityLocation2 = internalLocation(2L, prisonCode = prisonCode, description = "MDI-ACT-LOC2", userDescription = "Activity Location 2")
    private val appointmentLocation1 = appointmentLocation(123, prisonCode, description = "MDI-APP-LOC1", userDescription = "Appointment Location 1")
    private val visitsLocation = internalLocation(locationId = 5L, description = "MDI-VISIT-LOC", userDescription = "Visits Location")

    private val visit = PrisonApiPrisonerScheduleFixture.visitInstance(eventId = 8, locationId = visitsLocation.locationId, date = LocalDate.of(2022, 10, 1))

    @BeforeEach
    fun setUp() {
      prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1, activityLocation2, appointmentLocation1))
    }

    @Test
    fun `get location events authorisation required`() {
      val date = LocalDate.now()
      webTestClient.post()
        .uri("/scheduled-events/prison/$prisonCode/locations?date=$date")
        .bodyValue(setOf(1L))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get location events for date with activities, appointments and visits - 200 success`() {
      val internalLocationIds = setOf(activityLocation1.locationId, activityLocation2.locationId, appointmentLocation1.locationId, visitsLocation.locationId)
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1, activityLocation2, appointmentLocation1, visitsLocation))
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation1.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation2.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, appointmentLocation1.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, visitsLocation.locationId, date, null, listOf(visit))

      val result = webTestClient.getInternalLocationEvents(prisonCode, internalLocationIds, date)!!

      with(result) {
        size isEqualTo 4
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityLocation1.description
          description isEqualTo activityLocation1.userDescription
          events.single { it.scheduledInstanceId == 1L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityLocation2.description
          description isEqualTo activityLocation2.userDescription
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentLocation1.description
          description isEqualTo appointmentLocation1.userDescription
          events.single { it.appointmentAttendeeId == 5L }.eventType isEqualTo "APPOINTMENT"
        }
        with(this.single { it.id == visitsLocation.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo visitsLocation.description
          description isEqualTo visitsLocation.userDescription
          events.single { it.eventId == visit.eventId }.eventType isEqualTo "VISIT"
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get location events for date and time slot with one activity only - 200 success`() {
      val internalLocationIds = setOf(activityLocation1.locationId, activityLocation2.locationId, appointmentLocation1.locationId)
      val date = LocalDate.of(2022, 10, 1)
      val timeSlot = TimeSlot.PM

      prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1, activityLocation2, appointmentLocation1))
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation1.locationId, date, timeSlot, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation2.locationId, date, timeSlot, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, appointmentLocation1.locationId, date, timeSlot, emptyList())

      val result = webTestClient.getInternalLocationEvents(prisonCode, internalLocationIds, date, timeSlot)!!
      println(result)

      with(result) {
        size isEqualTo 3
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityLocation1.description
          description isEqualTo activityLocation1.userDescription
          events hasSize 0
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityLocation2.description
          description isEqualTo activityLocation2.userDescription
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentLocation1.description
          description isEqualTo appointmentLocation1.userDescription
          events hasSize 0
        }
      }
    }

    private fun WebTestClient.getInternalLocationEvents(
      prisonCode: String,
      internalLocationIds: Set<Long>,
      date: LocalDate,
      timeSlot: TimeSlot? = null,
    ) =
      post()
        .uri("/scheduled-events/prison/$prisonCode/locations?date=$date" + (timeSlot?.let { "&timeSlot=$timeSlot" } ?: ""))
        .bodyValue(internalLocationIds)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(InternalLocationEvents::class.java)
        .returnResult().responseBody
  }
}
