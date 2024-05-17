package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ORIGINAL_ID_PROPERTY_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
  ],
)
class AppointmentSeriesIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  private val telemetryCaptor = argumentCaptor<Map<String, String>>()

  @Test
  fun `get appointment series authorisation required`() {
    webTestClient.get()
      .uri("/appointment-series/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment series by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-series/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment series`() {
    val appointmentSeries = webTestClient.getAppointmentSeriesById(1)!!

    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        1,
        AppointmentType.INDIVIDUAL,
        "TPR",
        "AC1",
        EventTier(
          id = 2,
          code = "TIER_2",
          description = "Tier 2",
        ),
        EventOrganiser(
          id = 1,
          code = "PRISON_STAFF",
          description = "Prison staff",
        ),
        "Appointment description",
        123,
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        null,
        "Appointment series level comment",
        appointmentSeries.createdTime,
        "TEST.USER",
        null,
        null,
        appointments = listOf(
          Appointment(
            2,
            1,
            "TPR",
            "AC1",
            EventTier(
              id = 2,
              code = "TIER_2",
              description = "Tier 2",
            ),
            EventOrganiser(
              id = 1,
              code = "PRISON_STAFF",
              description = "Prison staff",
            ),
            "Appointment description",
            123,
            false,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            "Appointment level comment",
            appointmentSeries.createdTime,
            "TEST.USER",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                3,
                "A1234BC",
                456,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `get appointment series details authorisation required`() {
    webTestClient.get()
      .uri("/appointment-series/1/details")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment series details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-series/-1/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment series details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)

    val appointmentDetails = webTestClient.getAppointmentSeriesDetailsById(1)!!

    assertThat(appointmentDetails).isEqualTo(
      AppointmentSeriesDetails(
        1,
        AppointmentType.INDIVIDUAL,
        "TPR",
        "Appointment description (Appointment Category 1)",
        AppointmentCategorySummary("AC1", "Appointment Category 1"),
        EventTier(
          id = 2,
          code = "TIER_2",
          description = "Tier 2",
        ),
        EventOrganiser(
          id = 1,
          code = "PRISON_STAFF",
          description = "Prison staff",
        ),
        "Appointment description",
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        null,
        "Appointment series level comment",
        appointmentDetails.createdTime,
        "TEST.USER",
        null,
        null,
        appointments = listOf(
          AppointmentSummary(
            2,
            1,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            isEdited = false,
            isCancelled = false,
            isDeleted = false,
          ),
        ),
      ),
    )

    assertThat(appointmentDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `create appointment series authorisation required`() {
    webTestClient.post()
      .uri("/appointment-series")
      .bodyValue(appointmentSeriesCreateRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create appointment series single appointment single prisoner success for internal location`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "AC1")

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.created")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(attendeeIds[0]))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new appointment instance has been created in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series single appointment single prisoner success for in cell`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "AC1", internalLocationId = null, inCell = true)

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.created")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(attendeeIds[0]))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new appointment instance has been created in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series group appointment two prisoner success`() {
    val request = appointmentSeriesCreateRequest(
      categoryCode = "AC1",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = listOf("A12345BC", "B23456CE"),
    )

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A12345BC",
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "B23456CE",
          bookingId = 2,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentTwoPrisoner(appointmentSeries, request)
    assertSingleAppointmentTwoPrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(attendeeIds[0]),
      AppointmentInstanceInformation(attendeeIds[1]),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series duplicated from an original appointment`() {
    val request = appointmentSeriesCreateRequest(categoryCode = "AC1", internalLocationId = null, inCell = true, originalAppointmentId = 789L)

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertSingleAppointmentSinglePrisoner(appointmentSeries, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!, request)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.created")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(attendeeIds[0]))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new appointment instance has been created in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo "789"
    }
  }

  @Test
  fun `create individual repeat appointment series success`() {
    val request =
      appointmentSeriesCreateRequest(categoryCode = "AC1", schedule = AppointmentSeriesSchedule(AppointmentFrequency.FORTNIGHTLY, 3))

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumbers.first(),
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertThat(appointmentSeries.appointments).hasSize(3)

    verify(eventsPublisher, times(3)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(attendeeIds[0]),
      AppointmentInstanceInformation(attendeeIds[1]),
      AppointmentInstanceInformation(attendeeIds[2]),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series synchronously success`() {
    // 5 prisoners with 2 appointments results in 10 appointment instances. Lower than the configured max-sync-appointment-instance-actions value
    // The resulting create appointment request will be synchronous, creating all appointments and attendees
    val prisonerNumberToBookingIdMap = (1L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(
      categoryCode = "AC1",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(),
      schedule = AppointmentSeriesSchedule(AppointmentFrequency.DAILY, 2),
    )

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      prisonerNumberToBookingIdMap.map {
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = it.key,
          bookingId = it.value,
          prisonId = request.prisonCode!!,
        )
      },
    )

    val appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    val attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    appointmentSeries.appointments hasSize 2
    attendeeIds hasSize 10

    verify(eventsPublisher, times(attendeeIds.size)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).containsAll(
      attendeeIds.map { AppointmentInstanceInformation(it) },
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  @Test
  fun `create appointment series asynchronously success`() {
    // 3 prisoners with 4 appointments results in 12 appointment instances. Higher than the configured max-sync-appointment-instance-actions value
    // The resulting create appointment request will only create the first appointment and its attendees synchronously. The remaining
    // appointments and attendees will be created as an asynchronous job
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }

    val request = appointmentSeriesCreateRequest(
      categoryCode = "AC1",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(),
      schedule = AppointmentSeriesSchedule(AppointmentFrequency.DAILY, 4),
    )

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      prisonerNumberToBookingIdMap.map {
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = it.key,
          bookingId = it.value,
          prisonId = request.prisonCode!!,
        )
      },
    )

    // Synchronous creation. First appointment and attendees only
    var appointmentSeries = webTestClient.createAppointmentSeries(request)!!
    var attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }
    appointmentSeries.appointments hasSize 1
    attendeeIds hasSize 3

    // Wait for remaining appointments to be created
    Thread.sleep(1000)
    appointmentSeries = webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!
    attendeeIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }
    appointmentSeries.appointments hasSize 4
    attendeeIds hasSize 12

    verify(eventsPublisher, times(attendeeIds.size)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).containsAll(
      attendeeIds.map { AppointmentInstanceInformation(it) },
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())

    verify(telemetryClient).trackEvent(anyString(), telemetryCaptor.capture(), anyMap())

    with(telemetryCaptor.firstValue) {
      this[ORIGINAL_ID_PROPERTY_KEY] isEqualTo ""
    }
  }

  private fun assertSingleAppointmentSinglePrisoner(appointmentSeries: AppointmentSeries, request: AppointmentSeriesCreateRequest) {
    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        appointmentSeries.id,
        request.appointmentType!!,
        request.prisonCode!!,
        request.categoryCode!!,
        EventTier(
          id = appointmentSeries.tier!!.id,
          code = request.tierCode!!,
          description = appointmentSeries.tier!!.description,
        ),
        EventOrganiser(
          id = appointmentSeries.organiser!!.id,
          code = request.organiserCode!!,
          description = appointmentSeries.organiser!!.description,
        ),
        request.customName,
        request.internalLocationId,
        request.inCell,
        request.startDate!!,
        request.startTime!!,
        request.endTime,
        null,
        request.extraInformation,
        appointmentSeries.createdTime,
        "test-client",
        null,
        null,
        appointments = listOf(
          Appointment(
            appointmentSeries.appointments.first().id,
            1,
            request.prisonCode!!,
            request.categoryCode!!,
            EventTier(
              id = appointmentSeries.tier!!.id,
              code = request.tierCode!!,
              description = appointmentSeries.tier!!.description,
            ),
            EventOrganiser(
              id = appointmentSeries.organiser!!.id,
              code = request.organiserCode!!,
              description = appointmentSeries.organiser!!.description,
            ),
            request.customName,
            request.internalLocationId,
            request.inCell,
            request.startDate!!,
            request.startTime!!,
            request.endTime,
            request.extraInformation,
            appointmentSeries.createdTime,
            "test-client",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                appointmentSeries.appointments.first().attendees.first().id,
                request.prisonerNumbers.first(),
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(appointmentSeries.id).isGreaterThan(0)
    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(appointmentSeries.appointments.first().id).isGreaterThan(0)
    assertThat(appointmentSeries.appointments.first().attendees.first().id).isGreaterThan(0)
  }

  private fun assertSingleAppointmentTwoPrisoner(appointmentSeries: AppointmentSeries, request: AppointmentSeriesCreateRequest) {
    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        appointmentSeries.id,
        request.appointmentType!!,
        request.prisonCode!!,
        request.categoryCode!!,
        EventTier(
          id = appointmentSeries.tier!!.id,
          code = request.tierCode!!,
          description = appointmentSeries.tier!!.description,
        ),
        EventOrganiser(
          id = appointmentSeries.organiser!!.id,
          code = request.organiserCode!!,
          description = appointmentSeries.organiser!!.description,
        ),
        request.customName,
        request.internalLocationId,
        request.inCell,
        request.startDate!!,
        request.startTime!!,
        request.endTime,
        null,
        request.extraInformation,
        appointmentSeries.createdTime,
        "test-client",
        null,
        null,
        appointments = listOf(
          Appointment(
            appointmentSeries.appointments.first().id,
            1,
            request.prisonCode!!,
            request.categoryCode!!,
            EventTier(
              id = appointmentSeries.tier!!.id,
              code = request.tierCode!!,
              description = appointmentSeries.tier!!.description,
            ),
            EventOrganiser(
              id = appointmentSeries.organiser!!.id,
              code = request.organiserCode!!,
              description = appointmentSeries.organiser!!.description,
            ),
            request.customName,
            request.internalLocationId,
            request.inCell,
            request.startDate!!,
            request.startTime!!,
            request.endTime,
            request.extraInformation,
            appointmentSeries.createdTime,
            "test-client",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(id = 1, prisonerNumber = "A12345BC", bookingId = 1, null, null, null, null, null, null, null, null),
              AppointmentAttendee(id = 2, prisonerNumber = "B23456CE", bookingId = 2, null, null, null, null, null, null, null, null),
            ),
          ),
        ),
      ),
    )

    assertThat(appointmentSeries.id).isGreaterThan(0)
    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(appointmentSeries.appointments.first().id).isGreaterThan(0)
  }

  private fun WebTestClient.getAppointmentSeriesById(id: Long) =
    get()
      .uri("/appointment-series/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getAppointmentSeriesDetailsById(id: Long) =
    get()
      .uri("/appointment-series/$id/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeriesDetails::class.java)
      .returnResult().responseBody

  private fun WebTestClient.createAppointmentSeries(
    request: AppointmentSeriesCreateRequest,
  ) =
    post()
      .uri("/appointment-series")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, request.prisonCode)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody
}
