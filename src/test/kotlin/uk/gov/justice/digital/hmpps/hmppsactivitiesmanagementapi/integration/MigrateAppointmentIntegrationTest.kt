package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
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
class MigrateAppointmentIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  private lateinit var appointmentRepository: AppointmentRepository

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `migrate appointment success`() {
    val request = appointmentMigrateRequest(categoryCode = "AC1")

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf(request.prisonerNumber),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumber,
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val response = webTestClient.migrateAppointment(request)!!
    verifyAppointment(response)
    verifyDatabase()

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())

    assertThat(
      eventCaptor.allValues.map { it.eventType }.distinct().single(),
    ).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(response.occurrences[0].allocations[0].id),
    )
  }

  private fun verifyAppointment(response: Appointment) {
    assertThat(response.id).isNotNull
    assertThat(response.createdBy).isEqualTo("test-client")
    assertThat(response.created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(response.occurrences[0].allocations[0].prisonerNumber).isEqualTo("A1234BC")
    assertThat(response.categoryCode).isEqualTo("AC1")
    assertThat(response.prisonCode).isEqualTo("TPR")
    assertThat(response.internalLocationId).isEqualTo(123)
    assertThat(response.inCell).isFalse()
    assertThat(response.startDate).isEqualTo(LocalDate.now().plusDays(1))
    assertThat(response.startTime).isEqualTo(LocalTime.of(13, 0))
    assertThat(response.endTime).isEqualTo(LocalTime.of(14, 30))
    assertThat(response.comment).isEqualTo("Appointment level comment")
  }

  private fun verifyDatabase() {
    val appointments = appointmentRepository.findAll().map { it.toModel() }

    assertThat(appointments).hasSize(1)
    verifyAppointment(appointments.first())
  }

  private fun WebTestClient.migrateAppointment(
    request: AppointmentMigrateRequest,
  ) =
    post()
      .uri("/migrate-appointment")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody
}
