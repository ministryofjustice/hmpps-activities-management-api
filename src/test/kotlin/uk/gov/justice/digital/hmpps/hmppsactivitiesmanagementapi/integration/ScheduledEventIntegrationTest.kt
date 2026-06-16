package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.Hearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.externalMovement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class ScheduledEventIntegrationTest : IntegrationTestBase() {

  val locationUuid: UUID = UUID.fromString("88888888-8888-8888-8888-888888888888")

  @BeforeEach
  fun setUp() {
    // Stubs used to find category and location descriptions for appointments
    prisonApiMockServer.stubGetLocationsForTypeUnrestricted(
      "MDI",
      "APP",
      "prisonapi/locations-MDI-appointments.json",
    )

    val dpsLocation1 = dpsLocation(UUID.fromString("88888888-8888-8888-8888-888888888888"), "MDI", "ONE", "Location One")
    val dpsLocation2 = dpsLocation(UUID.fromString("99999999-9999-9999-9999-999999999999"), "MDI", "TWO", "Location Teo")

    val appointmentLocations = listOf(dpsLocation1, dpsLocation2)

    locationsInsidePrisonApiMockServer.stubLocationsForServiceType(
      prisonCode = "MDI",
      locations = appointmentLocations,
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation1.id, 1),
        NomisDpsLocationMapping(dpsLocation2.id, 2),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromDpsUuid(dpsLocation1.id)
  }

  @Nested
  inner class GetScheduledEventsForMultiplePrisoners {
    private val prisonCode = "MDI"
    private val prisonerNumbers = listOf("A11111A", "A22222A", "C11111A")
    private val date = LocalDate.of(2022, 10, 1)

    private fun WebTestClient.getScheduledEventsForMultiplePrisoners(
      prisonCode: String,
      prisonerNumbers: Set<String>,
      date: LocalDate,
    ) = post()
      .uri("/scheduled-events/prison/$prisonCode?date=$date")
      .bodyValue(prisonerNumbers)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerScheduledEvents::class.java)
      .returnResult().responseBody

    @BeforeEach
    fun setUp() {
      val activityLocation1 = internalLocation(1L, prisonCode = prisonCode, description = "MDI-ACT-LOC1", userDescription = "Activity Location 1")
      val activityLocation2 = internalLocation(2L, prisonCode = prisonCode, description = "MDI-ACT-LOC2", userDescription = "Activity Location 2")
      val appointmentLocation1 = appointmentLocation(123, prisonCode, description = "MDI-APP-LOC1", userDescription = "Appointment Location 1")

      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1, activityLocation2, appointmentLocation1))

      adjudicationsMock(prisonCode, date, prisonerNumbers)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `POST - multiple prisoners - activities active, appointments not active - 200 success`() {
      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(prisonerNumbers).contains("A11111A")
        assertThat(courtHearings).hasSize(2)
        assertThat(visits).hasSize(2)
        assertThat(activities).hasSize(2)

        with(activities!!.first { a -> a.prisonerNumber == "A11111A" }) {
          assertThat(eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(eventSource).isEqualTo("SAA")
          assertThat(priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
          assertThat(paidActivity).isTrue
          assertThat(issuePayment).isNull()
          assertThat(attendanceStatus).isEqualTo(AttendanceStatus.WAITING.name)
          assertThat(attendanceReasonCode).isEqualTo(AttendanceReasonEnum.NOT_REQUIRED.name)
        }

        with(activities!!.first { a -> a.prisonerNumber == "C11111A" }) {
          assertThat(eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(eventSource).isEqualTo("SAA")
          assertThat(priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
          assertThat(paidActivity).isTrue
          assertThat(issuePayment).isTrue
          assertThat(attendanceStatus).isEqualTo(AttendanceStatus.COMPLETED.name)
          assertThat(attendanceReasonCode).isEqualTo(AttendanceReasonEnum.ATTENDED.name)
        }

        assertThat(adjudications).hasSize(3)
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
      adjudicationsMock(prisonCode, date, prisonerNumbers)

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
    @Sql("classpath:test_data/seed-appointment-group-series-cancelled.sql")
    fun `POST - multiple prisoners - cancelled appointment series - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G4793VF", "A5193DY")
      val date = LocalDate.of(2022, 10, 16)

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(appointments).hasSize(2)
        appointments!!.map {
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
          assertThat(it.appointmentSeriesCancellationStartDate).isEqualTo(LocalDate.of(2022, 10, 16))
          assertThat(it.appointmentSeriesCancellationStartTime).isEqualTo(LocalTime.of(11, 30, 0))
          assertThat(it.appointmentSeriesFrequency).isEqualTo(AppointmentFrequency.DAILY)
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
    fun `POST - multiple prisoners - scheduled events with exclusions are not returned`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A5193DY")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(LocalDate.now())
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(13, 0))

        assertThat(externalTransfers).extracting("date", "startTime", "endTime").containsExactly(Tuple(date, LocalTime.of(0, 0), LocalTime.of(12, 0)))
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
    fun `POST - scheduled events with transfers without times are handled`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A5193DY")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date, includeTimes = false)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(externalTransfers).extracting("date", "startTime", "endTime").containsExactly(Tuple(date, null, null))
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-historical-exclusions.sql")
    fun `POST - multiple prisoners - scheduled events with exclusions are not returned - past date`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A5193DY")
      val date = LocalDate.now().minusDays(1)

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(date)
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(13, 0))
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-future-exclusions.sql")
    fun `POST - multiple prisoners - scheduled events with exclusions are not returned - future date`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A5193DY")
      val date = LocalDate.now().plusDays(1)

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(date)
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(13, 0))
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-prisoner-deallocated-on-same-day-as-session.sql")
    fun `POST - multiple prisoners - scheduled events not returned if the prisoner was deallocated earlier the same day`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G4793VF")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).isEmpty()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-planned-deallocation-date.sql")
    fun `POST - multiple prisoners - scheduled events returned on planned deallocation date`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G0459MM")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(date)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-planned-deallocation-date.sql")
    fun `POST - multiple prisoners - scheduled events not returned after planned deallocation date`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G0459MM")
      val date = LocalDate.now().plusDays(2)

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).isEmpty()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-planned-deallocation-date.sql")
    fun `POST - multiple prisoners - scheduled events should ignore past planned deallocations`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G0459NN")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(date)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-planned-deallocation-date.sql")
    fun `POST - multiple prisoners - scheduled events should return correct event when there's no planned deallocations`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G0459PP")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(date)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-planned-deallocation-date.sql")
    fun `POST - multiple prisoners - scheduled events should not return any activity events for today where attendance does not exist`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("G0459PP", "AA1111A")
      val date = LocalDate.now()

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers.toSet(), date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      adjudicationsMock(prisonCode, date, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), date)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(date)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-advance-attendances-2.sql")
    fun `POST - multiple prisoners - includes events where future not required is true`() {
      val prisonCode = "MDI"
      val prisonerNumbers = listOf("A11111A", "B22222B")
      val tomorrow = LocalDate.now().plusDays(1)

      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, tomorrow)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, tomorrow)
      adjudicationsMock(prisonCode, tomorrow, prisonerNumbers)

      val scheduledEvents = webTestClient.getScheduledEventsForMultiplePrisoners(prisonCode, prisonerNumbers.toSet(), tomorrow)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(3)

        with(activities!!.first { it.prisonerNumber == "A11111A" && it.scheduledInstanceId == 1L }) {
          assertThat(summary).isEqualTo("Geography AM")
          assertThat(date).isEqualTo(tomorrow)
          assertThat(attendanceReasonCode).isEqualTo("NOT_REQUIRED")
        }

        with(activities!!.first { it.prisonerNumber == "B22222B" && it.scheduledInstanceId == 1L }) {
          assertThat(summary).isEqualTo("Geography AM")
          assertThat(date).isEqualTo(tomorrow)
          assertThat(attendanceReasonCode).isNull()
        }

        with(activities!!.first { it.prisonerNumber == "B22222B" && it.scheduledInstanceId == 2L }) {
          assertThat(summary).isEqualTo("Maths AM")
          assertThat(date).isEqualTo(tomorrow)
          assertThat(attendanceReasonCode).isEqualTo("NOT_REQUIRED")
        }
      }
    }
  }

  @Nested
  @DisplayName("getInternalLocationEvents")
  inner class GetInternalLocationEvents {
    private val prisonCode = "MDI"

    private val activityLocation1 = internalLocation(
      1L,
      prisonCode = prisonCode,
      description = "MDI-ACT-LOC1",
      userDescription = "Activity Location 1",
    )
    private val activityLocation2 = internalLocation(
      2L,
      prisonCode = prisonCode,
      description = "MDI-ACT-LOC2",
      userDescription = "Activity Location 2",
    )
    private val appointmentLocation1 =
      appointmentLocation(123, prisonCode, description = "MDI-APP-LOC1", userDescription = "Appointment Location 1")
    private val visitsLocation =
      internalLocation(locationId = 5L, description = "MDI-VISIT-LOC", userDescription = "Visits Location")

    val dpsLocationId1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val dpsLocationId123 = UUID.fromString("12312312-1231-1231-1231-123123123123")
    val dpsLocationId5 = UUID.fromString("55555555-5555-5555-5555-555555555555")

    private val activityDpsLocation1 = dpsLocation(dpsLocationId1, prisonCode, "ACT-LOC1", "Activity Location 1")
    private val activityDpsLocation2 = dpsLocation(dpsLocationId2, prisonCode, "ACT-LOC2")
    private val appointmentDpsLocation1 =
      dpsLocation(dpsLocationId123, prisonCode, "APP-LOC1", "Appointment Location 1")
    private val visitsDpsLocation5 = dpsLocation(dpsLocationId5, prisonCode, "VISIT-LOC", "Visits Location")

    private val visit = PrisonApiPrisonerScheduleFixture.visitInstance(
      eventId = 8,
      locationId = visitsLocation.locationId,
      date = LocalDate.of(2022, 10, 1),
    )

    @BeforeEach
    fun setUp() {
      locationsInsidePrisonApiMockServer.stubNonResidentialLocations(
        prisonCode = prisonCode,
        locations = listOf(activityDpsLocation1, activityDpsLocation2, appointmentDpsLocation1, visitsDpsLocation5),
      )
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
    fun `get locations for date with activities, appointments and visits - 200 success`() {
      val internalLocationIds = setOf(
        activityLocation1.locationId,
        activityLocation2.locationId,
        appointmentLocation1.locationId,
        visitsLocation.locationId,
      )
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation1.locationId,
        date,
        null,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation2.locationId,
        date,
        null,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        appointmentLocation1.locationId,
        date,
        null,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        visitsLocation.locationId,
        date,
        null,
        listOf(visit),
      )
      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = date,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      val result = webTestClient.getLocations(prisonCode, internalLocationIds, date)!!

      with(result) {
        size isEqualTo 4
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events.filter { it.scheduledInstanceId == 1L && it.eventType == "ACTIVITY" } hasSize 2
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation2.code
          description isEqualTo activityDpsLocation2.localName
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentDpsLocation1.code
          description isEqualTo appointmentDpsLocation1.localName
          events.single { it.appointmentAttendeeId == 5L }.eventType isEqualTo "APPOINTMENT"
        }
        with(this.single { it.id == visitsLocation.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo visitsDpsLocation5.code
          description isEqualTo visitsDpsLocation5.localName
          events.single { it.eventId == visit.eventId }.eventType isEqualTo "VISIT"
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get location events for date with activities, appointments and visits - 200 success`() {
      val locationIds = setOf(
        activityDpsLocation1.id,
        activityDpsLocation2.id,
        appointmentDpsLocation1.id,
        visitsDpsLocation5.id,
      )
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation1.locationId,
        date,
        null,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation2.locationId,
        date,
        null,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        appointmentLocation1.locationId,
        date,
        null,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        visitsLocation.locationId,
        date,
        null,
        listOf(visit),
      )
      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = date,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      val result = webTestClient.getLocationEvents(prisonCode, locationIds, date)!!

      with(result) {
        size isEqualTo 4
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events.filter { it.scheduledInstanceId == 1L && it.eventType == "ACTIVITY" } hasSize 2
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation2.code
          description isEqualTo activityDpsLocation2.localName
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentDpsLocation1.code
          description isEqualTo appointmentDpsLocation1.localName
          events.single { it.appointmentAttendeeId == 5L }.eventType isEqualTo "APPOINTMENT"
        }
        with(this.single { it.id == visitsLocation.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo visitsDpsLocation5.code
          description isEqualTo visitsDpsLocation5.localName
          events.single { it.eventId == visit.eventId }.eventType isEqualTo "VISIT"
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get location events for single location with activities, appointments and visits - 200 success`() {
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation1.locationId,
        date,
        null,
        emptyList(),
      )
      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = date,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
        ),
      )

      locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(dpsLocationId1)

      val result = webTestClient.getLocationEvents(prisonCode, activityDpsLocation1.id, date)!!

      with(result) {
        size isEqualTo 1
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events.filter { it.scheduledInstanceId == 1L && it.eventType == "ACTIVITY" } hasSize 2
        }
      }
    }

    @Test
    fun `get location events for single invalid location - 404 not found`() {
      val date = LocalDate.of(2022, 10, 1)

      webTestClient.getLocationEventsLocationDoesNotExist(prisonCode, activityDpsLocation1.id, date)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-activity-id-3-on-wing.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get locations ignores activities with location id and on-wing is true`() {
      val internalLocationIds = setOf(activityLocation1.locationId, activityLocation2.locationId, appointmentLocation1.locationId, visitsLocation.locationId)
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation1.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation2.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, appointmentLocation1.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, visitsLocation.locationId, date, null, listOf(visit))
      manageAdjudicationsApiMockServer.stubHearingsForDate(agencyId = prisonCode, date = date, body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())))
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      val result = webTestClient.getLocations(prisonCode, internalLocationIds, date)!!

      with(result) {
        size isEqualTo 4
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events.filter { it.eventType == "ACTIVITY" } hasSize 2
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation2.code
          description isEqualTo activityDpsLocation2.localName
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentDpsLocation1.code
          description isEqualTo appointmentDpsLocation1.localName
          events.single { it.appointmentAttendeeId == 5L }.eventType isEqualTo "APPOINTMENT"
        }
        with(this.single { it.id == visitsLocation.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo visitsDpsLocation5.code
          description isEqualTo visitsDpsLocation5.localName
          events.single { it.eventId == visit.eventId }.eventType isEqualTo "VISIT"
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-activity-id-3-on-wing.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get location events ignores activities with location id and on-wing is true`() {
      val locationIds = setOf(
        activityDpsLocation1.id,
        activityDpsLocation2.id,
        appointmentDpsLocation1.id,
        visitsDpsLocation5.id,
      )
      val date = LocalDate.of(2022, 10, 1)

      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation1.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation2.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, appointmentLocation1.locationId, date, null, emptyList())
      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, visitsLocation.locationId, date, null, listOf(visit))
      manageAdjudicationsApiMockServer.stubHearingsForDate(agencyId = prisonCode, date = date, body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())))
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId5, visitsLocation.locationId),
        ),
      )

      val result = webTestClient.getLocationEvents(prisonCode, locationIds, date)!!

      with(result) {
        size isEqualTo 4
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events.filter { it.eventType == "ACTIVITY" } hasSize 2
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation2.code
          description isEqualTo activityDpsLocation2.localName
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentDpsLocation1.code
          description isEqualTo appointmentDpsLocation1.localName
          events.single { it.appointmentAttendeeId == 5L }.eventType isEqualTo "APPOINTMENT"
        }
        with(this.single { it.id == visitsLocation.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo visitsDpsLocation5.code
          description isEqualTo visitsDpsLocation5.localName
          events.single { it.eventId == visit.eventId }.eventType isEqualTo "VISIT"
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get locations for date and time slot with one activity only - 200 success`() {
      val internalLocationIds =
        setOf(activityLocation1.locationId, activityLocation2.locationId, appointmentLocation1.locationId)
      val date = LocalDate.of(2022, 10, 1)
      val timeSlot = TimeSlot.PM

      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation1.locationId,
        date,
        timeSlot,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation2.locationId,
        date,
        timeSlot,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        appointmentLocation1.locationId,
        date,
        timeSlot,
        emptyList(),
      )
      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = date,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
        ),
      )

      val result = webTestClient.getLocations(prisonCode, internalLocationIds, date, timeSlot)!!

      with(result) {
        size isEqualTo 3
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events hasSize 0
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation2.code
          description isEqualTo activityDpsLocation2.localName
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentDpsLocation1.code
          description isEqualTo appointmentDpsLocation1.localName
          events hasSize 0
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `get location events for date and time slot with one activity only - 200 success`() {
      val locationIds = setOf(
        activityDpsLocation1.id,
        activityDpsLocation2.id,
        appointmentDpsLocation1.id,
      )
      val date = LocalDate.of(2022, 10, 1)
      val timeSlot = TimeSlot.PM

      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation1.locationId,
        date,
        timeSlot,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        activityLocation2.locationId,
        date,
        timeSlot,
        emptyList(),
      )
      prisonApiMockServer.stubScheduledVisitsForLocation(
        prisonCode,
        appointmentLocation1.locationId,
        date,
        timeSlot,
        emptyList(),
      )
      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = date,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )
      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, activityLocation1.locationId),
          NomisDpsLocationMapping(dpsLocationId2, activityLocation2.locationId),
          NomisDpsLocationMapping(dpsLocationId123, appointmentLocation1.locationId),
        ),
      )

      val result = webTestClient.getLocationEvents(prisonCode, locationIds, date, timeSlot)!!

      with(result) {
        size isEqualTo 3
        with(this.single { it.id == activityLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events hasSize 0
        }
        with(this.single { it.id == activityLocation2.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation2.code
          description isEqualTo activityDpsLocation2.localName
          events.single { it.scheduledInstanceId == 6L }.eventType isEqualTo "ACTIVITY"
        }
        with(this.single { it.id == appointmentLocation1.locationId }) {
          prisonCode isEqualTo prisonCode
          code isEqualTo appointmentDpsLocation1.code
          description isEqualTo appointmentDpsLocation1.localName
          events hasSize 0
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-advance-attendances-2.sql")
    fun `get locations includes events where future not required is true`() {
      val internalLocationIds = setOf(1L)
      val tomorrow = LocalDate.now().plusDays(1)

      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, 1, tomorrow, null, emptyList())

      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = tomorrow,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )

      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, 1),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, 1),
        ),
      )

      val result = webTestClient.getLocations(prisonCode, internalLocationIds, tomorrow)!!

      with(result) {
        size isEqualTo 1

        with(this.first()) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events hasSize 3
          events.filter { it.prisonerNumber == "A11111A" || it.prisonerNumber == "C33333C" }
            .forEach {
              it.eventType isEqualTo "ACTIVITY"
              it.scheduledInstanceId isEqualTo 1L
              it.attendanceReasonCode isEqualTo "NOT_REQUIRED"
            }
          events.filter { it.prisonerNumber == "B22222B" }
            .forEach {
              it.eventType isEqualTo "ACTIVITY"
              it.scheduledInstanceId isEqualTo 1L
              it.attendanceReasonCode isEqualTo null
            }
        }
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-advance-attendances-2.sql")
    fun `get location events includes events where future not required is true`() {
      val locationIds = setOf(UUID.fromString("11111111-1111-1111-1111-111111111111"))
      val tomorrow = LocalDate.now().plusDays(1)

      prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, 1, tomorrow, null, emptyList())

      manageAdjudicationsApiMockServer.stubHearingsForDate(
        agencyId = prisonCode,
        date = tomorrow,
        body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())),
      )

      nomisMappingApiMockServer.stubMappingsFromNomisIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, 1),
        ),
      )

      nomisMappingApiMockServer.stubMappingsFromDpsIds(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, 1),
        ),
      )

      val result = webTestClient.getLocationEvents(prisonCode, locationIds, tomorrow)!!

      with(result) {
        size isEqualTo 1

        with(this.first()) {
          prisonCode isEqualTo prisonCode
          code isEqualTo activityDpsLocation1.code
          description isEqualTo activityDpsLocation1.localName
          events hasSize 3
          events.filter { it.prisonerNumber == "A11111A" || it.prisonerNumber == "C33333C" }
            .forEach {
              it.eventType isEqualTo "ACTIVITY"
              it.scheduledInstanceId isEqualTo 1L
              it.attendanceReasonCode isEqualTo "NOT_REQUIRED"
            }
          events.filter { it.prisonerNumber == "B22222B" }
            .forEach {
              it.eventType isEqualTo "ACTIVITY"
              it.scheduledInstanceId isEqualTo 1L
              it.attendanceReasonCode isEqualTo null
            }
        }
      }
    }

    private fun WebTestClient.getLocations(
      prisonCode: String,
      internalLocationIds: Set<Long>,
      date: LocalDate,
      timeSlot: TimeSlot? = null,
    ) = post()
      .uri(
        "/scheduled-events/prison/$prisonCode/locations?date=$date" + (
          timeSlot?.let { "&timeSlot=$timeSlot" }
            ?: ""
          ),
      )
      .bodyValue(internalLocationIds)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList<InternalLocationEvents>()
      .returnResult().responseBody

    private fun WebTestClient.getLocationEvents(
      prisonCode: String,
      dpsLocationIds: Set<UUID>,
      date: LocalDate,
      timeSlot: TimeSlot? = null,
    ) = post()
      .uri(
        "/scheduled-events/prison/$prisonCode/location-events?date=$date" + (
          timeSlot?.let { "&timeSlot=$timeSlot" }
            ?: ""
          ),
      )
      .bodyValue(dpsLocationIds)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList<InternalLocationEvents>()
      .returnResult().responseBody

    private fun WebTestClient.getLocationEvents(
      prisonCode: String,
      dpsLocationId: UUID,
      date: LocalDate,
      timeSlot: TimeSlot? = null,
    ) = get()
      .uri(
        "/scheduled-events/prison/$prisonCode/location-events?dpsLocationId=$dpsLocationId&date=$date" + (
          timeSlot?.let { "&timeSlot=$timeSlot" }
            ?: ""
          ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList<InternalLocationEvents>()
      .returnResult().responseBody
  }

  fun WebTestClient.getLocationEventsLocationDoesNotExist(
    prisonCode: String,
    dpsLocationId: UUID,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ) = get()
    .uri(
      "/scheduled-events/prison/$prisonCode/location-events?dpsLocationId=$dpsLocationId&date=$date" + (
        timeSlot?.let { "&timeSlot=$timeSlot" }
          ?: ""
        ),
    )
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isNotFound

  private fun adjudicationsMock(
    agencyId: String,
    date: LocalDate,
    prisonerNumbers: List<String>,
  ) {
    manageAdjudicationsApiMockServer.stubHearings(
      agencyId = agencyId,
      startDate = date,
      endDate = date,
      prisoners = prisonerNumbers,
      body = mapper.writeValueAsString(
        prisonerNumbers.mapIndexed { hearingId, offenderNo ->
          HearingsResponse(
            prisonerNumber = offenderNo,
            hearing = Hearing(
              id = hearingId.plus(1).toLong(),
              oicHearingType = "GOV_ADULT",
              dateTimeOfHearing = date.atTime(10, 30, 0),
              locationUuid = locationUuid,
              agencyId = agencyId,
            ),
          )
        },
      ),
    )
  }

  @Nested
  @DisplayName("External movements from External Movement API for unlock list")
  inner class ExternalMovementsFromExternalMovementApiForUnlockList {
    private val prisonCode = "MDI"
    private val prisonerNumbers = setOf("A11111A", "A22222A")
    private val date = LocalDate.now()

    @BeforeEach
    fun setUp() {
      val activityLocation1 = internalLocation(
        1L,
        prisonCode = prisonCode,
        description = "MDI-ACT-LOC1",
        userDescription = "Activity Location 1",
      )
      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers, date)
      prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1))
      adjudicationsMock(prisonCode, date, prisonerNumbers.toList())
    }

    @Test
    fun `returns external activities from external movement API when EA is rolled out and includeExternalMovements is true`() {
      val externalMovement = externalMovement()

      externalMovementsApiMockServer.stubGetExternalMovements(
        prisonCode,
        prisonerNumbers.toList(),
        date.atStartOfDay(),
        date.plusDays(1).atStartOfDay(),
        ExternalMovementsResponse(content = listOf(externalMovement)),
      )

      webTestClient.getScheduledEventsWithExternalActivities(
        prisonCode,
        prisonerNumbers,
        date,
        includeExternalMovements = true,
      )!!.activities!!
        .single { it.eventSource == "EXTERNAL_MOVEMENTS_API" }
        .apply {
          assertThat(summary).isEqualTo("Accommodation-related ROTL")
          assertThat(categoryDescription).isNull()
          assertThat(categoryCode).isEqualTo(externalMovement.description.code)
          assertThat(outsidePrison).isTrue()
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(17, 0))
          assertThat(status).isEqualTo(externalMovement.status.description)
        }
    }

    @Test
    fun `does not return external activities from external movement API when EA is rolled out and includeExternalMovements is false`() {
      val activities = webTestClient.getScheduledEventsWithExternalActivities(
        prisonCode,
        prisonerNumbers,
        date,
        includeExternalMovements = false,
      )!!.activities!!

      assertThat(
        activities.none { it.eventSource == "EXTERNAL_MOVEMENTS_API" },
      )
    }

    @ParameterizedTest(name = "includeExternalMovements = {0},")
    @ValueSource(booleans = [true, false])
    fun `does not return external activities from external movements API when prison is not rolled out for EA`(includeExternalMovements: Boolean) {
      val nonEaPrisonCode = "FMI"

      val activityLocation = internalLocation(1L, prisonCode = nonEaPrisonCode, description = "FMI-LOC1", userDescription = "Location 1")
      prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(nonEaPrisonCode, date)
      prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(nonEaPrisonCode, date)
      prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(nonEaPrisonCode, date)
      prisonApiMockServer.stubGetExternalTransfersOnDate(nonEaPrisonCode, prisonerNumbers, date)
      prisonApiMockServer.stubGetEventLocations(nonEaPrisonCode, listOf(activityLocation))
      prisonApiMockServer.stubGetLocationsForTypeUnrestricted(nonEaPrisonCode, "APP", "prisonapi/locations-MDI-appointments.json")
      locationsInsidePrisonApiMockServer.stubLocationsForServiceType(prisonCode = nonEaPrisonCode, locations = emptyList())
      adjudicationsMock(nonEaPrisonCode, date, prisonerNumbers.toList())

      val activities = webTestClient.getScheduledEventsWithExternalActivities(
        nonEaPrisonCode,
        prisonerNumbers,
        date,
        includeExternalMovements = includeExternalMovements,
      )!!.activities!!

      assertThat(activities.none { it.eventSource == "EXTERNAL_MOVEMENTS_API" })
    }

    private fun WebTestClient.getScheduledEventsWithExternalActivities(
      prisonCode: String,
      prisonerNumbers: Set<String>,
      date: LocalDate,
      includeExternalMovements: Boolean = false,
      timeSlot: TimeSlot? = null,
    ) = post()
      .uri(
        "/scheduled-events/prison/$prisonCode?date=$date&includeExternalMovements=$includeExternalMovements" +
          (timeSlot?.let { "&timeSlot=$it" } ?: ""),
      )
      .bodyValue(prisonerNumbers)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerScheduledEvents::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("External movements from External Movement API for movement list")
  inner class ExternalMovementsFromExternalMovementApiForMovementList {
    private val prisonCode = "MDI"
    private val date = LocalDate.now()

    @Test
    fun `returns external movements wrapped in LocationEvents`() {
      val externalMovement = externalMovement()

      externalMovementsApiMockServer.stubGetExternalMovements(
        prisonCode,
        start = date.atStartOfDay(),
        end = date.plusDays(1).atStartOfDay(),
        response = ExternalMovementsResponse(content = listOf(externalMovement)),
      )

      val result = webTestClient.getExternalMovements(prisonCode, date)

      with(result.single()) {
        assertThat(id).isNull()
        assertThat(dpsLocationId).isNull()
        assertThat(this.prisonCode).isEqualTo("MDI")
        assertThat(code).isEqualTo("OUTSIDE")
        assertThat(description).isEqualTo("Outside")
        with(events.single()) {
          assertThat(prisonerNumber).isEqualTo(externalMovement.prisonerNumber)
          assertThat(eventSource).isEqualTo("EXTERNAL_MOVEMENTS_API")
          assertThat(outsidePrison).isTrue()
          assertThat(categoryCode).isEqualTo(externalMovement.description.code)
          assertThat(summary).isEqualTo("Accommodation-related ROTL")
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(17, 0))
          assertThat(status).isEqualTo(externalMovement.status.description)
        }
      }
    }

    @Test
    fun `returns empty set when no external movements`() {
      externalMovementsApiMockServer.stubGetExternalMovements(
        prisonCode,
        start = date.atStartOfDay(),
        end = date.plusDays(1).atStartOfDay(),
        response = ExternalMovementsResponse(content = emptyList()),
      )

      val result = webTestClient.getExternalMovements(prisonCode, date)

      assertThat(result).isEmpty()
    }

    @Test
    fun `returns 401 when not authenticated`() {
      webTestClient.get()
        .uri("/scheduled-events/prison/$prisonCode/external-movements?date=$date")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    private fun WebTestClient.getExternalMovements(
      prisonCode: String,
      date: LocalDate,
      timeSlot: TimeSlot? = null,
    ) = get()
      .uri(
        "/scheduled-events/prison/$prisonCode/external-movements?date=$date" +
          (timeSlot?.let { "&timeSlot=$it" } ?: ""),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList<LocationEvents>()
      .returnResult().responseBody!!
  }
}
