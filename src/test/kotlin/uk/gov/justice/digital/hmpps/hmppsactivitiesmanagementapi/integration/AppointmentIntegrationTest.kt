package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentUncancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLY_TO_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EXTRA_INFORMATION_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ADDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_REMOVED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_TIME_CHANGED_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
    "feature.event.appointments.appointment-instance.updated=true",
    "feature.event.appointments.appointment-instance.deleted=true",
    "feature.event.appointments.appointment-instance.cancelled=true",
    "feature.event.appointments.appointment-instance.uncancelled=true",
  ],
)
class AppointmentIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient
  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @Test
  fun `update appointment by unknown id returns 404 not found`() {
    webTestClient.patch()
      .uri("/appointments/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .bodyValue(AppointmentUpdateRequest())
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `update single appointment`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "AC2",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)

    val appointmentSeries = webTestClient.updateAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(tier!!.code).isEqualTo("TIER_2")
      assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(customName).isEqualTo("Appointment description")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(appointments.single()) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(tier!!.code).isEqualTo("TIER_2")
        assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(extraInformation).isEqualTo(request.extraInformation)
        assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(attendees.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.updated")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(appointmentIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been updated in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `update single appointment to in cell`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "AC2",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      inCell = true,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    prisonApiMockServer.stubGetAppointmentScheduleReasons()

    val appointmentSeries = webTestClient.updateAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(tier!!.code).isEqualTo("TIER_2")
      assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(customName).isEqualTo("Appointment description")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(appointments.single()) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(tier!!.code).isEqualTo("TIER_2")
        assertThat(organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(internalLocationId).isNull()
        assertThat(inCell).isTrue()
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(extraInformation).isEqualTo(request.extraInformation)
        assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(attendees.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    verify(eventsPublisher).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.updated")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(appointmentIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been updated in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `cancel single appointment with a reason that does NOT trigger a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
      cancellationReasonId = 2,
    )

    val appointmentSeries = webTestClient.cancelAppointment(2, request)!!
    val appointmentIds = appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    with(appointmentSeries) {
      with(appointments.single()) {
        assertThat(cancellationReasonId).isEqualTo(2)
        assertThat(cancelledBy).isEqualTo("test-client")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
    }

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.cancelled")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(appointmentIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been cancelled in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `cancel single appointment with a reason that triggers a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
      cancellationReasonId = 1,
    )

    val originalAppointmentSeries = webTestClient.getAppointmentSeriesById(1)!!
    val appointmentIds = originalAppointmentSeries.appointments.filterNot { it.isDeleted }
      .flatMap { it.attendees.map { attendee -> attendee.id } }

    val appointmentSeries = webTestClient.cancelAppointment(2, request)!!

    assertThat(appointmentSeries.appointments.filterNot { it.isDeleted }).isEmpty()

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.deleted")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(appointmentIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been deleted in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `cancel group repeat appointment this and all future appointments with a reason that triggers a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
      cancellationReasonId = 1,
    )

    val appointmentSeries = webTestClient.cancelAppointment(12, request)!!

    with(appointmentSeries.appointments.filterNot { it.isDeleted }) {
      assertThat(map { it.cancellationReasonId }.distinct().single()).isNull()
      assertThat(map { it.cancelledBy }.distinct().single()).isNull()
      assertThat(map { it.cancelledTime }.distinct().single()).isNull()
      assertThat(subList(2, size)).isEmpty()
    }

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    val activeAppointments = appointmentSeries.appointments.filterNot { it.isDeleted }

    with(eventCaptor.allValues) {
      assertThat(map { it.additionalInformation }).containsAll(
        activeAppointments.subList(2, activeAppointments.size).flatMap {
          it.attendees.filter { attendee -> attendee.prisonerNumber == "C3456DE" }
            .map { attendee -> AppointmentInstanceInformation(attendee.id) }
        },
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(map { it.eventType }.distinct().single()).isEqualTo("appointments.appointment-instance.deleted")
      assertThat(
        map { it.description }.distinct().single(),
      ).isEqualTo("An appointment instance has been deleted in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `cancel group repeat appointment this and all future appointments with a reason that does NOT trigger a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
      cancellationReasonId = 2,
    )

    val appointmentSeries = webTestClient.cancelAppointment(12, request)!!

    with(appointmentSeries) {
      with(appointments.subList(0, 2)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.cancelledTime }.distinct().single()).isNull()
      }
      with(appointments.subList(2, appointments.size)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isEqualTo(2)
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("test-client")
        assertThat(map { it.cancelledTime }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
      }
    }

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.allValues) {
      assertThat(map { it.additionalInformation }).containsAll(
        appointmentSeries.appointments.subList(2, appointmentSeries.appointments.size).flatMap {
          it.attendees.filter { attendee -> attendee.prisonerNumber == "C3456DE" }
            .map { attendee -> AppointmentInstanceInformation(attendee.id) }
        },
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(map { it.eventType }.distinct().single()).isEqualTo("appointments.appointment-instance.cancelled")
      assertThat(
        map { it.description }.distinct().single(),
      ).isEqualTo("An appointment instance has been cancelled in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `uncancel a single appointment`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val appointmentSeries = webTestClient.uncancelAppointment(3, request)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody

    appointmentSeries.appointments.flatMap { it.attendees.map { attendee -> attendee.id } }

    assertThat(appointmentSeries.appointments).hasSize(4)

    with(appointmentSeries.appointments) {
      single { it.id == 3L }.isCancelled() isEqualTo false
      filter { it.id != 3L }
        .forEach {
          it.isCancelled() isEqualTo true
        }
    }

    verify(eventsPublisher).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.uncancelled")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(3))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been uncancelled in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentUncancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `uncancel a group appointment - THIS_AND_ALL_FUTURE_APPOINTMENTS`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
    )

    val appointmentSeries = webTestClient.uncancelAppointment(4, request)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody

    assertThat(appointmentSeries.appointments).hasSize(4)

    with(appointmentSeries.appointments) {
      single { it.id == 3L }.isCancelled() isEqualTo true
      single { it.id == 6L }.isCancelled() isEqualTo true
      filterNot { it.id == 3L || it.id == 6L }
        .forEach {
          it.isCancelled() isEqualTo false
        }
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.allValues) {
      single { it.additionalInformation == AppointmentInstanceInformation(4L) }
      single { it.additionalInformation.equals(AppointmentInstanceInformation(5L)) }
      forEach {
        assertThat(it.eventType).isEqualTo("appointments.appointment-instance.uncancelled")
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(it.description).isEqualTo("An appointment instance has been uncancelled in the activities management service")
      }
    }

    verify(auditService).logEvent(any<AppointmentUncancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `uncancel a group appointment - ALL_FUTURE_APPOINTMENTS`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val appointmentSeries = webTestClient.uncancelAppointment(4, request)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody

    assertThat(appointmentSeries.appointments).hasSize(4)

    with(appointmentSeries.appointments) {
      single { it.id == 6L }.isCancelled() isEqualTo true
      filterNot { it.id == 6L }
        .forEach {
          it.isCancelled() isEqualTo false
        }
    }

    verify(eventsPublisher, times(3)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.allValues) {
      single { it.additionalInformation == AppointmentInstanceInformation(3L) }
      single { it.additionalInformation == AppointmentInstanceInformation(4L) }
      single { it.additionalInformation == AppointmentInstanceInformation(5L) }
      forEach {
        assertThat(it.eventType).isEqualTo("appointments.appointment-instance.uncancelled")
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(it.description).isEqualTo("An appointment instance has been uncancelled in the activities management service")
      }
    }

    verify(auditService).logEvent(any<AppointmentUncancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-cancelled-id-1.sql",
  )
  @Test
  fun `400 - uncancel an appointment 6 days ago`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    val response = webTestClient.uncancelAppointment(6, request)!!

    response
      .expectStatus().isBadRequest
      .expectBody().jsonPath("developerMessage").isEqualTo("Cannot uncancel an appointment more than 5 days ago")
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `cancel large group repeat appointment location asynchronously success`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Cancelling all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // cancel only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be cancelled as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentCancelRequest(
      cancellationReasonId = 2,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    var appointmentSeries = webTestClient.cancelAppointment(appointmentId, request)!!

    // Synchronous cancel. Cancel specified appointment only
    with(appointmentSeries.appointments) {
      single { it.id == appointmentId }.isCancelled() isEqualTo true
      filter { it.id != appointmentId }.map { it.isCancelled() }.distinct().single() isEqualTo false
    }

    // Wait for remaining appointments to be cancelled
    Thread.sleep(1000)
    appointmentSeries = webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!
    appointmentSeries.appointments.map { it.isCancelled() }.distinct().single() isEqualTo true

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.cancelled" }) {
      assertThat(map { it.additionalInformation })
        .hasSameElementsAs(appointmentSeries.appointments.flatMap { it.attendees }.map { AppointmentInstanceInformation(it.id) })
    }

    verifyNoMoreInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_CANCELLED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verifyNoMoreInteractions(telemetryClient)

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `delete large group repeat appointment location asynchronously success`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Deleting all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // delete only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be deleted as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentCancelRequest(
      cancellationReasonId = 1,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    var appointmentSeries = webTestClient.cancelAppointment(appointmentId, request)!!

    // Synchronous delete. Delete specified appointment only
    with(appointmentSeries.appointments.filterNot { it.isDeleted }) {
      singleOrNull { it.id == appointmentId } isEqualTo null
      filter { it.id != appointmentId } hasSize 3
    }

    // Wait for remaining appointments to be deleted
    Thread.sleep(1000)
    appointmentSeries = webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!
    appointmentSeries.appointments.filterNot { it.isDeleted } hasSize 0

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      size isEqualTo 12
      assertThat(map { it.additionalInformation }).hasSameElementsAs(
        // The delete events for the specified appointment's instances are sent first
        listOf(36L, 37L, 38L)
          // Followed by the delete events for the remaining instances
          .union(listOf(30L, 31L, 32L, 33L, 34L, 35L, 39L, 40L, 41L))
          .map { AppointmentInstanceInformation(it) },
      )
    }

    verifyNoMoreInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_DELETED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verifyNoMoreInteractions(telemetryClient)
    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `update group repeat appointment this and all future appointments`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "AC2",
      tierCode = "TIER_2",
      organiserCode = "PRISON_STAFF",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      addPrisonerNumbers = listOf("B2345CD", "C3456DE"),
      removePrisonerNumbers = listOf("A1234BC"),
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
    )

    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.addPrisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 457, prisonId = "TPR"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 458, prisonId = "TPR"),
      ),
    )

    val appointmentSeries = webTestClient.updateAppointment(12, request)!!

    with(appointmentSeries) {
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(tier!!.code).isEqualTo("TIER_1")
      assertThat(organiser).isNull()
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(extraInformation).isEqualTo("Appointment series level comment")
      assertThat(updatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(appointments[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(appointments[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
      assertThat(appointments[2].startDate).isEqualTo(request.startDate)
      assertThat(appointments[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))

      val tier1Appointments = appointments.subList(0, 2)
      assertThat(tier1Appointments).hasSize(2)
      tier1Appointments.forEach {
        assertThat(it.categoryCode).isEqualTo("AC1")
        assertThat(it.tier!!.code).isEqualTo("TIER_1")
        assertThat(it.organiser).isNull()
        assertThat(it.internalLocationId).isEqualTo(123)
        assertThat(it.inCell).isFalse
        assertThat(it.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(10, 30))
        assertThat(it.extraInformation).isEqualTo("Appointment level comment")
        assertThat(it.updatedTime).isNull()
        assertThat(it.updatedBy).isNull()

        assertThat(it.attendees)
          .extracting(AppointmentAttendee::prisonerNumber, AppointmentAttendee::bookingId)
          .containsOnly(
            tuple("A1234BC", 456L),
            tuple("B2345CD", 457L),
          )
      }

      val tier2Appointments = appointments.subList(2, appointments.size)
      assertThat(tier2Appointments).hasSize(2)
      tier2Appointments.forEach {
        assertThat(it.categoryCode).isEqualTo(request.categoryCode)
        assertThat(it.tier!!.code).isEqualTo("TIER_2")
        assertThat(it.organiser!!.code).isEqualTo("PRISON_STAFF")
        assertThat(it.internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(it.inCell).isFalse
        assertThat(it.startTime).isEqualTo(request.startTime)
        assertThat(it.endTime).isEqualTo(request.endTime)
        assertThat(it.extraInformation).isEqualTo("Updated Appointment level comment")
        assertThat(it.updatedTime).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(it.updatedBy).isEqualTo("test-client")

        assertThat(it.attendees)
          .extracting(AppointmentAttendee::prisonerNumber, AppointmentAttendee::bookingId)
          .containsOnly(
            tuple("C3456DE", 458L),
            tuple("B2345CD", 457L),
          )
      }
    }

    verify(eventsPublisher, times(6)).send(eventCaptor.capture())
    verifyNoMoreInteractions(eventsPublisher)

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.created" }) {
      assertThat(size).isEqualTo(2)
      assertThat(map { it.additionalInformation }).containsAll(
        appointmentSeries.appointments.subList(2, appointmentSeries.appointments.size).flatMap {
          it.attendees.filter { attendee -> attendee.prisonerNumber == "C3456DE" }
            .map { attendee -> AppointmentInstanceInformation(attendee.id) }
        },
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(
        map { it.description }.distinct().single(),
      ).isEqualTo("A new appointment instance has been created in the activities management service")
    }

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.updated" }) {
      assertThat(size).isEqualTo(2)
      assertThat(map { it.additionalInformation }).contains(
        AppointmentInstanceInformation(25),
        AppointmentInstanceInformation(27),
      )
      forEach {
        assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
      assertThat(
        map { it.description }.distinct().single(),
      ).isEqualTo("An appointment instance has been updated in the activities management service")
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
      assertThat(
        map { it.description }.distinct().single(),
      ).isEqualTo("An appointment instance has been deleted in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `update large group repeat appointment location asynchronously success`() {
    // Seed appointment series has 4 appointments each with 3 attendees equalling 12 appointment instances. Editing all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // update only the first affected appointment and its attendees synchronously. The remaining appointments and attendees
    // will be updated as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentUpdateRequest(
      internalLocationId = 456,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)

    var appointmentSeries = webTestClient.updateAppointment(appointmentId, request)!!

    // Synchronous update. Update specified appointment only
    with(appointmentSeries.appointments) {
      single { it.id == appointmentId }.internalLocationId isEqualTo request.internalLocationId
      filter { it.id != appointmentId }.map { it.internalLocationId }.distinct().single() isEqualTo 123
    }

    // Wait for remaining appointments to be updated
    Thread.sleep(1000)
    appointmentSeries = webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!
    appointmentSeries.appointments.map { it.internalLocationId }.distinct().single() isEqualTo request.internalLocationId

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.updated" }) {
      assertThat(map { it.additionalInformation })
        .hasSameElementsAs(appointmentSeries.appointments.flatMap { it.attendees }.map { AppointmentInstanceInformation(it.id) })
    }

    verifyNoMoreInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_EDITED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("true")
      assertThat(this[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verifyNoMoreInteractions(telemetryClient)
    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `update large group repeat appointment attendees asynchronously success`() {
    // Seed appointment series has 4 appointments. Removing one prisoner and adding two new prisoners to all of them removes and adds
    // more attendees than the configured max-sync-appointment-instance-actions value. The service will therefore remove and
    // add attendees on only the first affected appointment and its attendees synchronously. The remaining appointments
    // will have attendees removed and added as an asynchronous job
    val appointmentId = 22L
    val request = AppointmentUpdateRequest(
      removePrisonerNumbers = listOf("A1234BC"),
      addPrisonerNumbers = listOf("D4567EF", "E5679FG"),
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      request.addPrisonerNumbers!!,
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 459, prisonId = "TPR"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5679FG", bookingId = 460, prisonId = "TPR"),
      ),
    )

    var appointmentSeries = webTestClient.updateAppointment(appointmentId, request)!!

    // Synchronous update. Update specified appointment only
    with(appointmentSeries.appointments) {
      assertThat(single { it.id == appointmentId }.attendees.map { it.prisonerNumber }).containsOnly("B2345CD", "C3456DE", "D4567EF", "E5679FG")
      assertThat(filter { it.id != appointmentId }.flatMap { it.attendees }.map { it.prisonerNumber }.distinct()).containsOnly("A1234BC", "B2345CD", "C3456DE")
    }

    // Wait for remaining appointments to be updated
    Thread.sleep(1000)
    appointmentSeries = webTestClient.getAppointmentSeriesById(appointmentSeries.id)!!
    assertThat(appointmentSeries.appointments.flatMap { it.attendees }.map { it.prisonerNumber }.distinct()).containsOnly("B2345CD", "C3456DE", "D4567EF", "E5679FG")

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      assertThat(size).isEqualTo(4)
      assertThat(map { it.additionalInformation }).containsExactly(
        // The deleted event for the specified appointment's attendee is sent first
        AppointmentInstanceInformation(36),
        // Followed by the deleted events for the remaining attendees
        AppointmentInstanceInformation(30),
        AppointmentInstanceInformation(33),
        AppointmentInstanceInformation(39),
      )
    }

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.created" }) {
      assertThat(map { it.additionalInformation })
        .hasSameElementsAs(
          appointmentSeries.appointments
            .flatMap { it.attendees }
            .filter { attendee -> listOf("E5679FG", "D4567EF").contains(attendee.prisonerNumber) }
            .map { AppointmentInstanceInformation(it.id) },
        )
    }

    verifyNoMoreInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_EDITED.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(this[APPOINTMENT_SERIES_ID_PROPERTY_KEY]).isEqualTo("7")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
      assertThat(this[CATEGORY_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[INTERNAL_LOCATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_DATE_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[START_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[END_TIME_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[EXTRA_INFORMATION_CHANGED_PROPERTY_KEY]).isEqualTo("false")
      assertThat(this[APPLY_TO_PROPERTY_KEY]).isEqualTo(request.applyTo.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_REMOVED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_ADDED_COUNT_METRIC_KEY]).isEqualTo(2.0)
      assertThat(this[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(4.0)
      assertThat(this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(12.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isNotNull
    }

    verifyNoMoreInteractions(telemetryClient)
    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  private fun WebTestClient.getAppointmentSeriesById(id: Long) = get()
    .uri("/appointment-series/$id")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.updateAppointment(
    id: Long,
    request: AppointmentUpdateRequest,
  ) = patch()
    .uri("/appointments/$id")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.cancelAppointment(
    id: Long,
    request: AppointmentCancelRequest,
  ) = put()
    .uri("/appointments/$id/cancel")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody

  private fun WebTestClient.uncancelAppointment(
    id: Long,
    request: AppointmentUncancelRequest,
  ) = put()
    .uri("/appointments/$id/uncancel")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
}
