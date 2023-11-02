package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreatedInErrorReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentCancelDomainServiceTest {
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val auditService: AuditService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val service = spy(
    AppointmentCancelDomainService(
      appointmentSeriesRepository,
      appointmentCancellationReasonRepository,
      telemetryClient,
      auditService,
      TransactionHandler(),
      outboundEventsService,
    ),
  )

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4)
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThis = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "")
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "")
  private val applyToAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")

  private val appointmentCancelledReason = appointmentCancelledReason()
  private val appointmentDeletedReason = appointmentCreatedInErrorReason()

  @BeforeEach
  fun setUp() {
    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))
    whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentCancelledReason),
    )
    whenever(appointmentCancellationReasonRepository.findById(appointmentDeletedReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentDeletedReason),
    )
    whenever(appointmentSeriesRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSeries>())
  }

  @Nested
  @DisplayName("cancel by ids - used by async cancel appointments job")
  inner class CancelAppointmentIds {
    @Test
    fun `cancels appointments with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentCancelRequest(cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId)
      val cancelled = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.cancelAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        cancelled,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      response.appointments.filter { ids.contains(it.id) }.map { it.isCancelled() }.distinct().single() isEqualTo true
      response.appointments.filterNot { ids.contains(it.id) }.map { it.isCancelled() }.distinct().single() isEqualTo false

      verify(service).cancelAppointments(
        appointmentSeries,
        appointment.appointmentId,
        applyToThisAndAllFuture.toSet(),
        request,
        cancelled,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
        true,
        false,
      )

      verifyNoInteractions(auditService)
    }

    @Test
    fun `track cancelled custom event using supplied counts and start time`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentCancelRequest(cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId)
      val startTimeInMs = System.currentTimeMillis()
      service.cancelAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        LocalDateTime.now(),
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_CANCELLED.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryMetricsMap.firstValue) {
        this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 3.0
        this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 10.0
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
      }

      verifyNoInteractions(auditService)
    }

    @Test
    fun `deletes appointments with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentCancelRequest(cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId)
      val cancelled = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.cancelAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        cancelled,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      response.appointments.filter { ids.contains(it.id) } hasSize 0
      response.appointments.filterNot { ids.contains(it.id) } hasSize 1

      verify(service).cancelAppointments(
        appointmentSeries,
        appointment.appointmentId,
        applyToThisAndAllFuture.toSet(),
        request,
        cancelled,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
        true,
        false,
      )

      verifyNoInteractions(auditService)
    }

    @Test
    fun `track deleted custom event using supplied counts and start time`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentCancelRequest(cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId)
      val startTimeInMs = System.currentTimeMillis()
      service.cancelAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        LocalDateTime.now(),
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_DELETED.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryMetricsMap.firstValue) {
        this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 3.0
        this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 10.0
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
      }

      verifyNoInteractions(auditService)
    }
  }

  @Test
  fun `appointment instance cancelled sync events raised on appointment update when appointment is cancelled`() {
    val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
    val request = AppointmentCancelRequest(cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId)
    val startTimeInMs = System.currentTimeMillis()
    service.cancelAppointmentIds(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      ids,
      request,
      LocalDateTime.now(),
      "TEST.USER",
      3,
      10,
      startTimeInMs,
    )

    applyToThisAndAllFuture.forEach {
      it.attendees().forEach { attendee ->
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, attendee.appointmentAttendeeId)
      }
    }

    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance deleted sync events raised on appointment update when appointment is deleted`() {
    val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
    val request = AppointmentCancelRequest(cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId)
    val startTimeInMs = System.currentTimeMillis()
    service.cancelAppointmentIds(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      ids,
      request,
      LocalDateTime.now(),
      "TEST.USER",
      3,
      10,
      startTimeInMs,
    )

    applyToThisAndAllFuture.forEach {
      it.attendees().forEach { attendee ->
        verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, attendee.appointmentAttendeeId)
      }
    }

    verifyNoMoreInteractions(outboundEventsService)
  }

  @Nested
  @DisplayName("instance count")
  inner class CancelInstanceCount {
    @Test
    fun `this appointment`() {
      service.getCancelInstancesCount(applyToThis) isEqualTo applyToThis.flatMap { it.attendees() }.size
    }

    @Test
    fun `this and all future appointments`() {
      service.getCancelInstancesCount(applyToThisAndAllFuture) isEqualTo applyToThisAndAllFuture.flatMap { it.attendees() }.size
    }

    @Test
    fun `all future appointments`() {
      service.getCancelInstancesCount(applyToAllFuture) isEqualTo applyToAllFuture.flatMap { it.attendees() }.size
    }
  }
}
