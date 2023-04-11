package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentInstanceUpdatedInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
    "feature.event.appointments.appointment-instance.updated=true",
    "feature.event.appointments.appointment-instance.deleted=true",
  ],
)
class AppointmentOccurrenceIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: EventsPublisher
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `update appointment occurrence authorisation required`() {
    webTestClient.patch()
      .uri("/appointment-occurrences/1")
      .bodyValue(AppointmentOccurrenceUpdateRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `update appointment occurrence by unknown id returns 404 not found`() {
    webTestClient.patch()
      .uri("/appointment-occurrences/-1")
      .headers(setAuthorisation(roles = listOf()))
      .bodyValue(AppointmentOccurrenceUpdateRequest())
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `update single appointment occurrence`() {
    val request = AppointmentOccurrenceUpdateRequest(
      categoryCode = "AC2",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      comment = "Updated appointment occurrence level comment",
      prisonerNumbers = listOf("A1234BC"),
      applyTo = ApplyTo.THIS_OCCURRENCE,
    )

    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers!!.first(), bookingId = 456, prisonId = "TPR"),
      ),
    )

    val appointment = webTestClient.updateAppointmentOccurrence(2, request)!!
    val allocationIds = appointment.occurrences.flatMap { it.allocations.map { allocation -> allocation.id } }

    with(appointment) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment level comment")
      assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(occurrences.single()) {
        assertThat(internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(comment).isEqualTo(request.comment)
        assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(allocations.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.updated")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceUpdatedInformation(allocationIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been updated in the activities management service")
    }
  }

  private fun WebTestClient.updateAppointmentOccurrence(
    id: Long,
    request: AppointmentOccurrenceUpdateRequest,
  ) =
    patch()
      .uri("/appointment-occurrences/$id")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody
}
