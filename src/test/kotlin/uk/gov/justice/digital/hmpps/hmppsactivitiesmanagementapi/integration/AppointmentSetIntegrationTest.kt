package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
  ],
)
class AppointmentSetIntegrationTest : AppointmentsIntegrationTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  @Test
  fun `get appointment set details authorisation required`() {
    webTestClient.get()
      .uri("/appointment-set/1/details")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment set details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-set/-1/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-set-id-6.sql",
  )
  @Test
  fun `get appointment set details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()

    val dpsLocation = dpsLocation(UUID.fromString("44444444-1111-2222-3333-444444444444"), "TPR", localName = "Test Appointment Location")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "TPR",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, 123),
      ),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC", "B2345CD", "C3456DE"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST01",
          lastName = "PRISONER01",
          prisonId = "TPR",
          cellLocation = "1-2-3",
          category = "H",
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "B2345CD",
          bookingId = 457,
          firstName = "TEST02",
          lastName = "PRISONER02",
          prisonId = "TPR",
          cellLocation = "1-2-4",
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "C3456DE",
          bookingId = 458,
          firstName = "TEST03",
          lastName = "PRISONER03",
          prisonId = "TPR",
          cellLocation = "1-2-5",
          category = "A",
        ),
      ),
    )

    val details = webTestClient.getAppointmentSetDetailsById(6)!!

    val category = AppointmentCategorySummary("AC1", "Appointment Category 1")
    val customName = "Appointment description"
    val createdBy = "TEST.USER"
    assertThat(details).isEqualTo(
      AppointmentSetDetails(
        6,
        "TPR",
        "$customName (${category.description})",
        category,
        customName,
        AppointmentLocationSummary(123, dpsLocation.id, "TPR", "Test Appointment Location"),
        false,
        LocalDate.now().plusDays(1),
        appointments = listOf(
          appointmentDetails(
            6, null, AppointmentSetSummary(6, 3, 3), 1,
            listOf(
              PrisonerSummary("A1234BC", 456, "TEST01", "PRISONER01", "ACTIVE IN", "TPR", "1-2-3", "H"),
            ),
            category, customName,
            LocalTime.of(9, 0),
            LocalTime.of(9, 15),
            "Medical appointment for A1234BC",
            details.createdTime, createdBy, null, null,
            appointmentAttendeeId = 6,
          ),
          appointmentDetails(
            7, null, AppointmentSetSummary(6, 3, 3), 1,
            listOf(
              PrisonerSummary("B2345CD", 457, "TEST02", "PRISONER02", "ACTIVE IN", "TPR", "1-2-4", "P"),
            ),
            category, customName,
            LocalTime.of(9, 15),
            LocalTime.of(9, 30),
            "Medical appointment for B2345CD",
            details.createdTime, createdBy, null, null,
            appointmentAttendeeId = 7,
          ),
          appointmentDetails(
            8, null, AppointmentSetSummary(6, 3, 3), 1,
            listOf(
              PrisonerSummary("C3456DE", 458, "TEST03", "PRISONER03", "ACTIVE IN", "TPR", "1-2-5", "A"),
            ),
            category, customName,
            LocalTime.of(9, 30),
            LocalTime.of(9, 45),
            "Medical appointment for C3456DE",
            details.createdTime, createdBy, null, null,
            appointmentAttendeeId = 8,
          ),
        ),
        details.createdTime,
        createdBy,
        null,
        null,
      ),
    )

    assertThat(details.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `create appointment set success for internal location`() {
    val request = appointmentSetCreateRequest(categoryCode = "AC1")
    val prisonerNumbers = request.appointments.map { it.prisonerNumber!! }.toList()
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = prisonerNumbers[0],
          bookingId = 1,
          prisonId = request.prisonCode,
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = prisonerNumbers[1],
          bookingId = 2,
          prisonId = request.prisonCode,
        ),
      ),
    )

    val response = webTestClient.createAppointmentSet(request)!!
    verifyAppointmentSet(request, response)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(response.appointments[0].attendees[0].id),
      AppointmentInstanceInformation(response.appointments[1].attendees[0].id),
    )

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), any(), any())

    verify(auditService).logEvent(any<AppointmentSetCreatedEvent>())
  }

  @Test
  fun `create appointment set success for in cell`() {
    val request = appointmentSetCreateRequest(categoryCode = "AC1", internalLocationId = null, inCell = true)
    val prisonerNumbers = request.appointments.map { it.prisonerNumber!! }.toList()
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = prisonerNumbers[0],
          bookingId = 1,
          prisonId = request.prisonCode,
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = prisonerNumbers[1],
          bookingId = 2,
          prisonId = request.prisonCode,
        ),
      ),
    )

    val response = webTestClient.createAppointmentSet(request)!!
    verifyAppointmentSet(request, response)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(response.appointments[0].attendees[0].id),
      AppointmentInstanceInformation(response.appointments[1].attendees[0].id),
    )

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_SET_CREATED.value), any(), any())

    verify(auditService).logEvent(any<AppointmentSetCreatedEvent>())
  }

  private fun verifyAppointmentSet(request: AppointmentSetCreateRequest, response: AppointmentSet) {
    assertThat(response.id).isNotNull
    assertThat(response.appointments).hasSize(request.appointments.size)
    assertThat(response.createdBy).isEqualTo("test-client")
    assertThat(response.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))

    assertThat(response.appointments.map { it.attendees.first().prisonerNumber }).isEqualTo(request.appointments.map { it.prisonerNumber })

    response.appointments.forEach {
      assertThat(it.categoryCode).isEqualTo(request.categoryCode)
      assertThat(it.prisonCode).isEqualTo(request.prisonCode)
      assertThat(it.internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(it.inCell).isEqualTo(request.inCell)
      assertThat(it.startDate).isEqualTo(request.startDate)
      assertThat(it.startTime).isEqualTo(request.appointments.first().startTime)
      assertThat(it.endTime).isEqualTo(request.appointments.first().endTime)
      assertThat(it.customName).isEqualTo("Appointment description")
    }
  }

  private fun WebTestClient.createAppointmentSet(
    request: AppointmentSetCreateRequest,
  ) = post()
    .uri("/appointment-set")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSet::class.java)
    .returnResult().responseBody
}
