package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSeriesCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
  ],
)
class AppointmentIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `get appointment authorisation required`() {
    webTestClient.get()
      .uri("/appointments/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment`() {
    val appointment = webTestClient.getAppointmentById(1)!!

    assertThat(appointment).isEqualTo(
      AppointmentSeries(
        1,
        AppointmentType.INDIVIDUAL,
        "TPR",
        "AC1",
        "Appointment description",
        123,
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        null,
        "Appointment series level comment",
        appointment.createdTime,
        "TEST.USER",
        null,
        null,
        appointments = listOf(
          Appointment(
            2,
            1,
            "TPR",
            "AC1",
            "Appointment description",
            123,
            false,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            "Appointment level comment",
            appointment.createdTime,
            "TEST.USER",
            null,
            null,
            null,
            null,
            null,
            allocations = listOf(
              AppointmentAttendee(
                3,
                "A1234BC",
                456,
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(appointment.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `get appointment by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `create appointment authorisation required`() {
    webTestClient.post()
      .uri("/appointments")
      .bodyValue(appointmentSeriesCreateRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create appointment single appointment single prisoner success`() {
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

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.appointments.flatMap { it.allocations.map { allocation -> allocation.id } }

    assertSingleAppointmentSinglePrisoner(appointment, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentById(appointment.id)!!, request)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.created")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(allocationIds[0]))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new appointment instance has been created in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())
  }

  @Test
  fun `create appointment group appointment two prisoner success`() {
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

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.appointments.flatMap { it.allocations.map { allocation -> allocation.id } }

    assertSingleAppointmentTwoPrisoner(appointment, request)
    assertSingleAppointmentTwoPrisoner(webTestClient.getAppointmentById(appointment.id)!!, request)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(allocationIds[0]),
      AppointmentInstanceInformation(allocationIds[1]),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())
  }

  @Test
  fun `create individual repeat appointment success`() {
    val request =
      appointmentSeriesCreateRequest(categoryCode = "AC1", schedule = AppointmentSchedule(AppointmentFrequency.FORTNIGHTLY, 3))

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

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.appointments.flatMap { it.allocations.map { allocation -> allocation.id } }

    assertThat(appointment.appointments).hasSize(3)

    verify(eventsPublisher, times(3)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(allocationIds[0]),
      AppointmentInstanceInformation(allocationIds[1]),
      AppointmentInstanceInformation(allocationIds[2]),
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())
  }

  @Test
  fun `create appointment synchronously success`() {
    // 5 prisoners with 2 occurrences results in 10 appointment instances. Lower than the configured max-sync-appointment-instance-actions value
    // The resulting create appointment request will be synchronous, creating all occurrences and allocations
    val prisonerNumberToBookingIdMap = (1L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
    val request = appointmentSeriesCreateRequest(
      categoryCode = "AC1",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(),
      schedule = AppointmentSchedule(AppointmentFrequency.DAILY, 2),
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

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.appointments.flatMap { it.allocations.map { allocation -> allocation.id } }

    appointment.appointments hasSize 2
    allocationIds hasSize 10

    verify(eventsPublisher, times(allocationIds.size)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).containsAll(
      allocationIds.map { AppointmentInstanceInformation(it) },
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())
  }

  @Test
  fun `create appointment asynchronously success`() {
    // 3 prisoners with 4 occurrences results in 12 appointment instances. Higher than the configured max-sync-appointment-instance-actions value
    // The resulting create appointment request will only create the first occurrence and its allocations synchronously. The remaining
    // occurrences and allocations will be created as an asynchronous job
    val prisonerNumberToBookingIdMap = (1L..3L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }

    val request = appointmentSeriesCreateRequest(
      categoryCode = "AC1",
      appointmentType = AppointmentType.GROUP,
      prisonerNumbers = prisonerNumberToBookingIdMap.keys.toList(),
      schedule = AppointmentSchedule(AppointmentFrequency.DAILY, 4),
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

    // Synchronous creation. First occurrence and allocations only
    val appointment = webTestClient.createAppointment(request)!!
    var allocationIds = appointment.appointments.flatMap { it.allocations.map { allocation -> allocation.id } }
    appointment.appointments hasSize 1
    allocationIds hasSize 3

    // Wait for remaining occurrences to be created
    Thread.sleep(1000)
    val appointmentDetails = webTestClient.getAppointmentById(appointment.id)!!
    allocationIds = appointmentDetails.appointments.flatMap { it.allocations.map { allocation -> allocation.id } }
    appointmentDetails.appointments hasSize 4
    allocationIds hasSize 12

    verify(eventsPublisher, times(allocationIds.size)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).containsAll(
      allocationIds.map { AppointmentInstanceInformation(it) },
    )

    verify(auditService).logEvent(any<AppointmentSeriesCreatedEvent>())
  }

  private fun assertSingleAppointmentSinglePrisoner(appointmentSeries: AppointmentSeries, request: AppointmentSeriesCreateRequest) {
    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        appointmentSeries.id,
        request.appointmentType!!,
        request.prisonCode!!,
        request.categoryCode!!,
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
            allocations = listOf(
              AppointmentAttendee(
                appointmentSeries.appointments.first().allocations.first().id,
                request.prisonerNumbers.first(),
                1,
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(appointmentSeries.id).isGreaterThan(0)
    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(appointmentSeries.appointments.first().id).isGreaterThan(0)
    assertThat(appointmentSeries.appointments.first().allocations.first().id).isGreaterThan(0)
  }

  private fun assertSingleAppointmentTwoPrisoner(appointmentSeries: AppointmentSeries, request: AppointmentSeriesCreateRequest) {
    assertThat(appointmentSeries).isEqualTo(
      AppointmentSeries(
        appointmentSeries.id,
        request.appointmentType!!,
        request.prisonCode!!,
        request.categoryCode!!,
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
            allocations = listOf(
              AppointmentAttendee(id = 1, prisonerNumber = "A12345BC", bookingId = 1),
              AppointmentAttendee(id = 2, prisonerNumber = "B23456CE", bookingId = 2),
            ),
          ),
        ),
      ),
    )

    assertThat(appointmentSeries.id).isGreaterThan(0)
    assertThat(appointmentSeries.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(appointmentSeries.appointments.first().id).isGreaterThan(0)
  }

  private fun WebTestClient.getAppointmentById(id: Long) =
    get()
      .uri("/appointments/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody

  private fun WebTestClient.createAppointment(
    request: AppointmentSeriesCreateRequest,
  ) =
    post()
      .uri("/appointments")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, request.prisonCode)
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody
}
