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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier
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

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
  ],
)
class AppointmentSetIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @Test
  fun `get appointment set authorisation required`() {
    webTestClient.get()
      .uri("/appointment-set/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment set by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-set/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-set-id-6.sql",
  )
  @Test
  fun `get appointment set`() {
    val appointmentSet = webTestClient.getAppointmentSetById(6)!!

    assertThat(appointmentSet).isEqualTo(
      AppointmentSet(
        6,
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
        appointments = listOf(
          Appointment(
            6,
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
            LocalTime.of(9, 15),
            "Medical appointment for A1234BC",
            appointmentSet.appointments[0].createdTime,
            "TEST.USER",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                6,
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
          Appointment(
            7,
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
            LocalTime.of(9, 15),
            LocalTime.of(9, 30),
            "Medical appointment for B2345CD",
            appointmentSet.appointments[1].createdTime,
            "TEST.USER",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                7,
                "B2345CD",
                457,
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
          Appointment(
            8,
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
            LocalTime.of(9, 30),
            LocalTime.of(9, 45),
            "Medical appointment for C3456DE",
            appointmentSet.appointments[2].createdTime,
            "TEST.USER",
            null,
            null,
            null,
            null,
            null,
            isDeleted = false,
            attendees = listOf(
              AppointmentAttendee(
                8,
                "C3456DE",
                458,
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
        appointmentSet.createdTime,
        "TEST.USER",
      ),
    )

    assertThat(appointmentSet.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

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
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
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
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
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
    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
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
    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
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

  private fun WebTestClient.getAppointmentSetById(id: Long) =
    get()
      .uri("/appointment-set/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSet::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getAppointmentSetDetailsById(id: Long) =
    get()
      .uri("/appointment-set/$id/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSetDetails::class.java)
      .returnResult().responseBody

  private fun WebTestClient.createAppointmentSet(
    request: AppointmentSetCreateRequest,
  ) =
    post()
      .uri("/appointment-set")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSet::class.java)
      .returnResult().responseBody
}
