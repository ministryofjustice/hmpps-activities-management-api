package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentInstanceInformation
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
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR"),
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
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(allocationIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been updated in the activities management service")
    }
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `update group repeat appointment this and all future occurrences`() {
    val request = AppointmentOccurrenceUpdateRequest(
      categoryCode = "AC2",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      comment = "Updated appointment occurrence level comment",
      prisonerNumbers = listOf("B2345CD", "C3456DE"),
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES,
    )

    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 457, prisonId = "TPR"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 458, prisonId = "TPR"),
      ),
    )

    val appointment = webTestClient.updateAppointmentOccurrence(12, request)!!

    with(appointment) {
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment level comment")
      assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(occurrences[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(occurrences[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
      assertThat(occurrences[2].startDate).isEqualTo(request.startDate)
      assertThat(occurrences[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))
      with(occurrences.subList(0, 2)) {
        assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
        assertThat(map { it.inCell }.distinct().single()).isFalse
        assertThat(map { it.startTime }.distinct().single()).isEqualTo(LocalTime.of(9, 0))
        assertThat(map { it.endTime }.distinct().single()).isEqualTo(LocalTime.of(10, 30))
        assertThat(map { it.comment }.distinct().single()).isEqualTo("Appointment occurrence level comment")
        assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("test-client")
        assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("A1234BC")
        assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(456)
        assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
        assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(457)
      }
      with(occurrences.subList(2, occurrences.size)) {
        assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(request.internalLocationId)
        assertThat(map { it.inCell }.distinct().single()).isFalse
        assertThat(map { it.startTime }.distinct().single()).isEqualTo(request.startTime)
        assertThat(map { it.endTime }.distinct().single()).isEqualTo(request.endTime)
        assertThat(map { it.comment }.distinct().single()).isEqualTo("Updated appointment occurrence level comment")
        assertThat(map { it.updated }.distinct().single()).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("test-client")
        assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
        assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(457)
        assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("C3456DE")
        assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(458)
      }
    }

    verify(eventsPublisher, times(10)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.created" }) {
      assertThat(size).isEqualTo(2)
      assertThat(map { it.additionalInformation }).containsAll(
        appointment.occurrences.subList(2, appointment.occurrences.size).flatMap {
          it.allocations.filter { allocation -> allocation.prisonerNumber == "C3456DE" }
            .map { allocation -> AppointmentInstanceInformation(allocation.id) }
        },
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(map { it.description }.distinct().single()).isEqualTo("A new appointment instance has been created in the activities management service")
    }

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.updated" }) {
      assertThat(size).isEqualTo(6)
      assertThat(map { it.additionalInformation }).contains(
        AppointmentInstanceInformation(20),
        AppointmentInstanceInformation(21),
        AppointmentInstanceInformation(22),
        AppointmentInstanceInformation(23),
        AppointmentInstanceInformation(25),
        AppointmentInstanceInformation(27),
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(map { it.description }.distinct().single()).isEqualTo("An appointment instance has been updated in the activities management service")
    }

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      assertThat(size).isEqualTo(2)
      assertThat(map { it.additionalInformation }).contains(
        AppointmentInstanceInformation(24),
        AppointmentInstanceInformation(26),
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(map { it.description }.distinct().single()).isEqualTo("An appointment instance has been deleted in the activities management service")
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
