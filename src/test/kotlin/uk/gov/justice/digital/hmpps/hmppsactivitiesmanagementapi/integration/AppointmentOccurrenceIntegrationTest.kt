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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
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
  ],
)
class AppointmentOccurrenceIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockBean
  private lateinit var telemetryClient: TelemetryClient
  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @Test
  fun `update appointment occurrence authorisation required`() {
    webTestClient.patch()
      .uri("/appointment-occurrences/1")
      .bodyValue(AppointmentUpdateRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `update appointment occurrence by unknown id returns 404 not found`() {
    webTestClient.patch()
      .uri("/appointment-occurrences/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .bodyValue(AppointmentUpdateRequest())
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `update single appointment occurrence`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "AC2",
      internalLocationId = 456,
      startDate = LocalDate.now().plusDays(3),
      startTime = LocalTime.of(13, 30),
      endTime = LocalTime.of(15, 0),
      extraInformation = "Updated Appointment level comment",
      applyTo = ApplyTo.THIS_APPOINTMENT,
    )

    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)

    val appointment = webTestClient.updateAppointmentOccurrence(2, request)!!
    val allocationIds = appointment.occurrences.flatMap { it.allocations.map { allocation -> allocation.id } }

    with(appointment) {
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment series level comment")
      assertThat(appointmentDescription).isEqualTo("Appointment description")
      assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      with(occurrences.single()) {
        assertThat(categoryCode).isEqualTo(request.categoryCode)
        assertThat(internalLocationId).isEqualTo(request.internalLocationId)
        assertThat(inCell).isFalse
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(startTime).isEqualTo(request.startTime)
        assertThat(endTime).isEqualTo(request.endTime)
        assertThat(comment).isEqualTo(request.extraInformation)
        assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(updatedBy).isEqualTo("test-client")
        with(allocations.single()) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
        }
      }
    }

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.updated")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(allocationIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been updated in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentEditedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `cancel single appointment occurrence with a reason that does NOT trigger a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
      cancellationReasonId = 2,
    )

    val appointment = webTestClient.cancelAppointmentOccurrence(2, request)!!
    val allocationIds = appointment.occurrences.flatMap { it.allocations.map { allocation -> allocation.id } }

    with(appointment) {
      with(occurrences.single()) {
        assertThat(cancellationReasonId).isEqualTo(2)
        assertThat(cancelledBy).isEqualTo("test-client")
        assertThat(cancelled).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      }
    }

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.cancelled")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(allocationIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been cancelled in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentCancelledEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `cancel single appointment occurrence with a reason that triggers a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_APPOINTMENT,
      cancellationReasonId = 1,
    )

    val originalAppointment = webTestClient.getAppointmentById(1)!!
    val allocationIds = originalAppointment.occurrences
      .flatMap { it.allocations.map { allocation -> allocation.id } }

    val updatedAppointment = webTestClient.cancelAppointmentOccurrence(2, request)!!

    assertThat(updatedAppointment.occurrences).isEmpty()

    verify(eventsPublisher, times(1)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("appointments.appointment-instance.deleted")
      assertThat(additionalInformation).isEqualTo(AppointmentInstanceInformation(allocationIds.first()))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An appointment instance has been deleted in the activities management service")
    }

    verify(auditService).logEvent(any<AppointmentDeletedEvent>())
  }

  @Sql(
    "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  )
  @Test
  fun `cancel group repeat appointment this and all future occurrences with a reason that triggers a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
      cancellationReasonId = 1,
    )

    val appointment = webTestClient.cancelAppointmentOccurrence(12, request)!!

    with(appointment) {
      with(occurrences.subList(0, 2)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.cancelled }.distinct().single()).isNull()
      }
      assertThat(occurrences.subList(2, occurrences.size)).isEmpty()
    }

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    with(eventCaptor.allValues) {
      assertThat(map { it.additionalInformation }).containsAll(
        appointment.occurrences.subList(2, appointment.occurrences.size).flatMap {
          it.allocations.filter { allocation -> allocation.prisonerNumber == "C3456DE" }
            .map { allocation -> AppointmentInstanceInformation(allocation.id) }
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
  fun `cancel group repeat appointment this and all future occurrences with a reason that does NOT trigger a soft delete`() {
    val request = AppointmentCancelRequest(
      applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS,
      cancellationReasonId = 2,
    )

    val appointment = webTestClient.cancelAppointmentOccurrence(12, request)!!

    with(appointment) {
      with(occurrences.subList(0, 2)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isNull()
        assertThat(map { it.cancelledBy }.distinct().single()).isNull()
        assertThat(map { it.cancelled }.distinct().single()).isNull()
      }
      with(occurrences.subList(2, occurrences.size)) {
        assertThat(map { it.cancellationReasonId }.distinct().single()).isEqualTo(2)
        assertThat(map { it.cancelledBy }.distinct().single()).isEqualTo("test-client")
        assertThat(map { it.cancelled }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
      }
    }

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    with(eventCaptor.allValues) {
      assertThat(map { it.additionalInformation }).containsAll(
        appointment.occurrences.subList(2, appointment.occurrences.size).flatMap {
          it.allocations.filter { allocation -> allocation.prisonerNumber == "C3456DE" }
            .map { allocation -> AppointmentInstanceInformation(allocation.id) }
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
    "classpath:test_data/seed-appointment-group-repeat-12-instances-id-7.sql",
  )
  @Test
  fun `cancel large group repeat appointment location asynchronously success`() {
    // Seed appointment has 4 occurrences each with 3 allocations equalling 12 appointment instances. Cancelling all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // cancel only the first affected occurrence and its allocations synchronously. The remaining occurrences and allocations
    // will be cancelled as an asynchronous job
    val appointmentOccurrenceId = 22L
    val request = AppointmentCancelRequest(
      cancellationReasonId = 2,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val appointment = webTestClient.cancelAppointmentOccurrence(appointmentOccurrenceId, request)!!

    // Synchronous cancel. Cancel specified occurrence only
    with(appointment.occurrences) {
      single { it.id == appointmentOccurrenceId }.isCancelled() isEqualTo true
      filter { it.id != appointmentOccurrenceId }.map { it.isCancelled() }.distinct().single() isEqualTo false
    }

    // Wait for remaining occurrences to be cancelled
    Thread.sleep(1000)
    val appointmentDetails = webTestClient.getAppointmentById(appointment.id)!!
    appointmentDetails.occurrences.map { it.isCancelled() }.distinct().single() isEqualTo true

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.cancelled" }) {
      size isEqualTo 12
      assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        // The cancel events for the specified occurrence's instances are sent first
        appointmentDetails.occurrences.single { it.id == appointmentOccurrenceId }.allocations.map { AppointmentInstanceInformation(it.id) }
          // Followed by the cancel events for the remaining instances
          .union(appointmentDetails.occurrences.filter { it.id != appointmentOccurrenceId }.flatMap { it.allocations }.map { AppointmentInstanceInformation(it.id) }),
      )
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
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentOccurrenceId.toString())
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
    // Seed appointment has 4 occurrences each with 3 allocations equalling 12 appointment instances. Deleting all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // delete only the first affected occurrence and its allocations synchronously. The remaining occurrences and allocations
    // will be deleted as an asynchronous job
    val appointmentOccurrenceId = 22L
    val request = AppointmentCancelRequest(
      cancellationReasonId = 1,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    val appointment = webTestClient.cancelAppointmentOccurrence(appointmentOccurrenceId, request)!!

    // Synchronous delete. Delete specified occurrence only
    with(appointment.occurrences) {
      singleOrNull { it.id == appointmentOccurrenceId } isEqualTo null
      filter { it.id != appointmentOccurrenceId } hasSize 3
    }

    // Wait for remaining occurrences to be deleted
    Thread.sleep(1000)
    val appointmentDetails = webTestClient.getAppointmentById(appointment.id)!!
    appointmentDetails.occurrences hasSize 0

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      size isEqualTo 12
      assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        // The delete events for the specified occurrence's instances are sent first
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
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentOccurrenceId.toString())
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
  fun `update group repeat appointment this and all future occurrences`() {
    val request = AppointmentUpdateRequest(
      categoryCode = "AC2",
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

    val appointment = webTestClient.updateAppointmentOccurrence(12, request)!!

    with(appointment) {
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse
      assertThat(startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment series level comment")
      assertThat(updated).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(occurrences[0].startDate).isEqualTo(LocalDate.now().minusDays(3))
      assertThat(occurrences[1].startDate).isEqualTo(LocalDate.now().minusDays(3).plusWeeks(1))
      assertThat(occurrences[2].startDate).isEqualTo(request.startDate)
      assertThat(occurrences[3].startDate).isEqualTo(request.startDate!!.plusWeeks(1))
      with(occurrences.subList(0, 2)) {
        assertThat(map { it.categoryCode }.distinct().single()).isEqualTo("AC1")
        assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(123)
        assertThat(map { it.inCell }.distinct().single()).isFalse
        assertThat(map { it.startTime }.distinct().single()).isEqualTo(LocalTime.of(9, 0))
        assertThat(map { it.endTime }.distinct().single()).isEqualTo(LocalTime.of(10, 30))
        assertThat(map { it.comment }.distinct().single()).isEqualTo("Appointment level comment")
        assertThat(map { it.updated }.distinct().single()).isNull()
        assertThat(map { it.updatedBy }.distinct().single()).isNull()
        assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("A1234BC")
        assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(456)
        assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
        assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(457)
      }
      with(occurrences.subList(2, occurrences.size)) {
        assertThat(map { it.categoryCode }.distinct().single()).isEqualTo(request.categoryCode)
        assertThat(map { it.internalLocationId }.distinct().single()).isEqualTo(request.internalLocationId)
        assertThat(map { it.inCell }.distinct().single()).isFalse
        assertThat(map { it.startTime }.distinct().single()).isEqualTo(request.startTime)
        assertThat(map { it.endTime }.distinct().single()).isEqualTo(request.endTime)
        assertThat(map { it.comment }.distinct().single()).isEqualTo("Updated Appointment level comment")
        assertThat(map { it.updated }.distinct().single()).isCloseTo(
          LocalDateTime.now(),
          within(60, ChronoUnit.SECONDS),
        )
        assertThat(map { it.updatedBy }.distinct().single()).isEqualTo("test-client")
        assertThat(map { it.allocations[0].prisonerNumber }.distinct().single()).isEqualTo("B2345CD")
        assertThat(map { it.allocations[0].bookingId }.distinct().single()).isEqualTo(457)
        assertThat(map { it.allocations[1].prisonerNumber }.distinct().single()).isEqualTo("C3456DE")
        assertThat(map { it.allocations[1].bookingId }.distinct().single()).isEqualTo(458)
      }
    }

    verify(eventsPublisher, times(6)).send(eventCaptor.capture())

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
    // Seed appointment has 4 occurrences each with 3 allocations equalling 12 appointment instances. Editing all of them
    // affects more instances than the configured max-sync-appointment-instance-actions value. The service will therefore
    // update only the first affected occurrence and its allocations synchronously. The remaining occurrences and allocations
    // will be updated as an asynchronous job
    val appointmentOccurrenceId = 22L
    val request = AppointmentUpdateRequest(
      internalLocationId = 456,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )

    prisonApiMockServer.stubGetLocationsForAppointments("TPR", request.internalLocationId!!)

    val appointment = webTestClient.updateAppointmentOccurrence(appointmentOccurrenceId, request)!!

    // Synchronous update. Update specified occurrence only
    with(appointment.occurrences) {
      single { it.id == appointmentOccurrenceId }.internalLocationId isEqualTo request.internalLocationId
      filter { it.id != appointmentOccurrenceId }.map { it.internalLocationId }.distinct().single() isEqualTo 123
    }

    // Wait for remaining occurrences to be updated
    Thread.sleep(1000)
    val appointmentDetails = webTestClient.getAppointmentById(appointment.id)!!
    appointmentDetails.occurrences.map { it.internalLocationId }.distinct().single() isEqualTo request.internalLocationId

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.updated" }) {
      size isEqualTo 12
      assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        // The update events for the specified occurrence's instances are sent first
        appointmentDetails.occurrences.single { it.id == appointmentOccurrenceId }.allocations.map { AppointmentInstanceInformation(it.id) }
          // Followed by the update events for the remaining instances
          .union(appointmentDetails.occurrences.filter { it.id != appointmentOccurrenceId }.flatMap { it.allocations }.map { AppointmentInstanceInformation(it.id) }),
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
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentOccurrenceId.toString())
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
  fun `update large group repeat appointment allocation asynchronously success`() {
    // Seed appointment has 4 occurrences. Removing one prisoner and adding two new prisoners to all of them removes and adds
    // more allocations than the configured max-sync-appointment-instance-actions value. The service will therefore remove and
    // add allocations on only the first affected occurrence and its allocations synchronously. The remaining occurrences
    // will have allocations removed and added as an asynchronous job
    val appointmentOccurrenceId = 22L
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

    val appointment = webTestClient.updateAppointmentOccurrence(appointmentOccurrenceId, request)!!

    // Synchronous update. Update specified occurrence only
    with(appointment.occurrences) {
      assertThat(single { it.id == appointmentOccurrenceId }.allocations.map { it.prisonerNumber }).containsOnly("B2345CD", "C3456DE", "D4567EF", "E5679FG")
      assertThat(filter { it.id != appointmentOccurrenceId }.flatMap { it.allocations }.map { it.prisonerNumber }.distinct()).containsOnly("A1234BC", "B2345CD", "C3456DE")
    }

    // Wait for remaining occurrences to be updated
    Thread.sleep(1000)
    val appointmentDetails = webTestClient.getAppointmentById(appointment.id)!!
    assertThat(appointmentDetails.occurrences.flatMap { it.allocations }.map { it.prisonerNumber }.distinct()).containsOnly("B2345CD", "C3456DE", "D4567EF", "E5679FG")

    verify(eventsPublisher, times(12)).send(eventCaptor.capture())

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.deleted" }) {
      assertThat(size).isEqualTo(4)
      assertThat(map { it.additionalInformation }).containsExactly(
        // The deleted event for the specified occurrence's allocation is sent first
        AppointmentInstanceInformation(36),
        // Followed by the deleted events for the remaining allocations
        AppointmentInstanceInformation(30),
        AppointmentInstanceInformation(33),
        AppointmentInstanceInformation(39),
      )
    }

    with(eventCaptor.allValues.filter { it.eventType == "appointments.appointment-instance.created" }) {
      assertThat(size).isEqualTo(8)
      assertThat(map { it.additionalInformation }).containsExactlyElementsOf(
        // The create events for the specified occurrence's new allocations are sent first
        appointmentDetails.occurrences.single { it.id == appointmentOccurrenceId }.allocations.filter { allocation -> listOf("D4567EF", "E5679FG").contains(allocation.prisonerNumber) }.map { AppointmentInstanceInformation(it.id) }
          // Followed by the create events for the remaining allocations
          .union(appointmentDetails.occurrences.filter { it.id != appointmentOccurrenceId }.flatMap { it.allocations }.filter { allocation -> listOf("D4567EF", "E5679FG").contains(allocation.prisonerNumber) }.map { AppointmentInstanceInformation(it.id) }),
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
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentOccurrenceId.toString())
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

  private fun WebTestClient.getAppointmentById(id: Long) =
    get()
      .uri("/appointments/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody

  private fun WebTestClient.updateAppointmentOccurrence(
    id: Long,
    request: AppointmentUpdateRequest,
  ) =
    patch()
      .uri("/appointment-occurrences/$id")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody

  private fun WebTestClient.cancelAppointmentOccurrence(
    id: Long,
    request: AppointmentCancelRequest,
  ) =
    put()
      .uri("/appointment-occurrences/$id/cancel")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody
}
