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
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentSetCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSetCreateRequest
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
class BulkAppointmentIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `create bulk appointments success`() {
    val request = appointmentSetCreateRequest(categoryCode = "AC1")
    val prisonerNumbers = request.appointments.map { it.prisonerNumber }.toList()
    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode, request.internalLocationId)
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

    val response = webTestClient.bulkCreateAppointments(request)!!
    verifyBulkAppointment(response)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(response.appointments[0].attendees[0].id),
      AppointmentInstanceInformation(response.appointments[1].attendees[0].id),
    )

    verify(auditService).logEvent(any<AppointmentSetCreatedEvent>())
  }

  private fun verifyBulkAppointment(response: AppointmentSet) {
    assertThat(response.id).isNotNull
    assertThat(response.appointments).hasSize(2)
    assertThat(response.createdBy).isEqualTo("test-client")
    assertThat(response.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))

    assertThat(response.appointments[0].attendees[0].prisonerNumber).isEqualTo("A1234BC")
    assertThat(response.appointments[1].attendees[0].prisonerNumber).isEqualTo("A1234BD")

    response.appointments.forEach {
      assertThat(it.categoryCode).isEqualTo("AC1")
      assertThat(it.prisonCode).isEqualTo("TPR")
      assertThat(it.internalLocationId).isEqualTo(123)
      assertThat(it.inCell).isFalse()
      assertThat(it.startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(it.startTime).isEqualTo(LocalTime.of(13, 0))
      assertThat(it.endTime).isEqualTo(LocalTime.of(14, 30))
      assertThat(it.customName).isEqualTo("Appointment description")
    }
  }

  private fun WebTestClient.bulkCreateAppointments(
    request: AppointmentSetCreateRequest,
  ) =
    post()
      .uri("/bulk-appointments")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSet::class.java)
      .returnResult().responseBody
}
