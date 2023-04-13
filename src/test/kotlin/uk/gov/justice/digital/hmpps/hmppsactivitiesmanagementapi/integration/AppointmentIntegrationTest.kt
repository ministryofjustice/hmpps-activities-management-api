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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
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
  ],
)
class AppointmentIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: EventsPublisher
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
    val appointment = webTestClient.getAppointmentById(1)

    with(appointment!!) {
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(prisonCode).isEqualTo("TPR")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isEqualTo(false)
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment level comment")
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST.USER")
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      assertThat(schedule).isNull()
      with(occurrences) {
        assertThat(size).isEqualTo(1)
        with(get(0)) {
          assertThat(internalLocationId).isEqualTo(123)
          assertThat(inCell).isEqualTo(false)
          assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
          assertThat(comment).isEqualTo("Appointment occurrence level comment")
          assertThat(cancelled).isEqualTo(false)
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          with(allocations) {
            assertThat(size).isEqualTo(1)
            with(get(0)) {
              assertThat(prisonerNumber).isEqualTo("A1234BC")
              assertThat(bookingId).isEqualTo(456)
            }
          }
        }
      }
    }
  }

  @Test
  fun `get appointment by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments/-1")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-deleted-id-2.sql",
  )
  @Test
  fun `get deleted appointment returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments/2")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `create appointment authorisation required`() {
    webTestClient.post()
      .uri("/appointments")
      .bodyValue(appointmentCreateRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create appointment single appointment single prisoner success`() {
    val request = appointmentCreateRequest(categoryCode = "AC1")

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
      ),
    )

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.occurrences.flatMap { it.allocations.map { allocation -> allocation.id } }

    assertSingleAppointmentSinglePrisoner(appointment, request)
    assertSingleAppointmentSinglePrisoner(webTestClient.getAppointmentById(appointment.id)!!, request)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.created")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(allocationIds[0]))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new appointment instance has been created in the activities management service")
    }
  }

  @Test
  fun `create appointment group appointment two prisoner success`() {
    val request = appointmentCreateRequest(
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
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A12345BC", bookingId = 1, prisonId = request.prisonCode!!),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B23456CE", bookingId = 1, prisonId = request.prisonCode!!),
      ),
    )

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.occurrences.flatMap { it.allocations.map { allocation -> allocation.id } }

    assertSingleAppointmentTwoPrisoner(appointment, request)
    assertSingleAppointmentTwoPrisoner(webTestClient.getAppointmentById(appointment.id)!!, request)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(eventCaptor.allValues.map { it.eventType }.distinct().single()).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(allocationIds[0]),
      AppointmentInstanceInformation(allocationIds[1]),
    )
  }

  @Test
  fun `create individual repeat appointment success`() {
    val request = appointmentCreateRequest(categoryCode = "AC1", repeat = AppointmentRepeat(AppointmentRepeatPeriod.FORTNIGHTLY, 3))

    prisonApiMockServer.stubGetUserCaseLoads(request.prisonCode!!)
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments(request.prisonCode!!, request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.prisonerNumbers,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = request.prisonerNumbers.first(), bookingId = 1, prisonId = request.prisonCode!!),
      ),
    )

    val appointment = webTestClient.createAppointment(request)!!
    val allocationIds = appointment.occurrences.flatMap { it.allocations.map { allocation -> allocation.id } }

    with(appointment) {
      with(occurrences) {
        assertThat(size).isEqualTo(3)
      }
    }

    verify(eventsPublisher, times(3)).send(eventCaptor.capture())

    assertThat(eventCaptor.allValues.map { it.eventType }.distinct().single()).isEqualTo("appointments.appointment-instance.created")
    assertThat(eventCaptor.allValues.map { it.additionalInformation }).contains(
      AppointmentInstanceInformation(allocationIds[0]),
      AppointmentInstanceInformation(allocationIds[1]),
      AppointmentInstanceInformation(allocationIds[2]),
    )
  }

  private fun assertSingleAppointmentSinglePrisoner(appointment: Appointment, request: AppointmentCreateRequest) {
    with(appointment) {
      assertThat(id).isGreaterThan(0)
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(inCell).isEqualTo(request.inCell)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(appointmentType).isEqualTo(AppointmentType.INDIVIDUAL)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("test-client")
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      assertThat(schedule).isNull()
      with(occurrences) {
        assertThat(size).isEqualTo(1)
        with(occurrences.first()) {
          assertThat(id).isGreaterThan(0)
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isEqualTo(request.inCell)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isNull()
          assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo("test-client")
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          with(allocations) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(id).isGreaterThan(0)
              assertThat(prisonerNumber).isEqualTo(request.prisonerNumbers.first())
              assertThat(bookingId).isEqualTo(1)
            }
          }
        }
      }
    }
  }

  private fun assertSingleAppointmentTwoPrisoner(appointment: Appointment, request: AppointmentCreateRequest) {
    with(appointment) {
      assertThat(id).isGreaterThan(0)
      assertThat(categoryCode).isEqualTo(request.categoryCode)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(internalLocationId).isEqualTo(request.internalLocationId)
      assertThat(inCell).isEqualTo(request.inCell)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(startTime).isEqualTo(request.startTime)
      assertThat(endTime).isEqualTo(request.endTime)
      assertThat(appointmentType).isEqualTo(AppointmentType.GROUP)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("test-client")
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      assertThat(schedule).isNull()
      with(occurrences) {
        assertThat(size).isEqualTo(1)
        with(occurrences.first()) {
          assertThat(id).isGreaterThan(0)
          assertThat(categoryCode).isEqualTo(request.categoryCode)
          assertThat(prisonCode).isEqualTo(request.prisonCode)
          assertThat(internalLocationId).isEqualTo(request.internalLocationId)
          assertThat(inCell).isEqualTo(request.inCell)
          assertThat(startDate).isEqualTo(request.startDate)
          assertThat(startTime).isEqualTo(request.startTime)
          assertThat(endTime).isEqualTo(request.endTime)
          assertThat(comment).isNull()
          assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
          assertThat(createdBy).isEqualTo("test-client")
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
          with(allocations) {
            assertThat(size).isEqualTo(2)
          }
          assertThat(
            allocations.containsAll(
              listOf(
                AppointmentOccurrenceAllocation(id = 1, prisonerNumber = "A12345BC", bookingId = 1),
                AppointmentOccurrenceAllocation(id = 2, prisonerNumber = "B23456CE", bookingId = 2),
              ),
            ),
          )
        }
      }
    }
  }

  private fun WebTestClient.getAppointmentById(id: Long) =
    get()
      .uri("/appointments/$id")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody

  private fun WebTestClient.createAppointment(
    request: AppointmentCreateRequest,
  ) =
    post()
      .uri("/appointments")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody
}
