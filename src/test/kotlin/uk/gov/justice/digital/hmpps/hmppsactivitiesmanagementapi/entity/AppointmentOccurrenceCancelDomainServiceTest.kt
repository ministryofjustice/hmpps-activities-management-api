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
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDeletedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentOccurrenceCancelDomainServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val auditService: AuditService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val service = spy(AppointmentOccurrenceCancelDomainService(appointmentRepository, appointmentCancellationReasonRepository, telemetryClient, auditService))

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentFrequency.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointmentSeries.appointments()[1]
  private val applyToThis = appointmentSeries.applyToAppointments(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "")
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "")
  private val applyToAllFuture = appointmentSeries.applyToAppointments(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")

  private val appointmentCancelledReason = appointmentCancelledReason()
  private val appointmentDeletedReason = appointmentDeletedReason()

  @BeforeEach
  fun setUp() {
    whenever(appointmentRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))
    whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentCancelledReason),
    )
    whenever(appointmentCancellationReasonRepository.findById(appointmentDeletedReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentDeletedReason),
    )
    whenever(appointmentRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSeries>())
  }

  @Nested
  @DisplayName("cancel by ids - used by async cancel appointment occurrences job")
  inner class CancelAppointmentOccurrenceIds {
    @Test
    fun `cancels occurrences with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceCancelRequest(cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId)
      val cancelled = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.cancelAppointmentOccurrenceIds(
        appointmentSeries.appointmentSeriesId,
        appointmentOccurrence.appointmentOccurrenceId,
        ids,
        request,
        cancelled,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      response.occurrences.filter { ids.contains(it.id) }.map { it.isCancelled() }.distinct().single() isEqualTo true
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.isCancelled() }.distinct().single() isEqualTo false

      verify(service).cancelAppointmentOccurrences(
        appointmentSeries,
        appointmentOccurrence.appointmentOccurrenceId,
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
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceCancelRequest(cancellationReasonId = appointmentCancelledReason.appointmentCancellationReasonId)
      val startTimeInMs = System.currentTimeMillis()
      service.cancelAppointmentOccurrenceIds(
        appointmentSeries.appointmentSeriesId,
        appointmentOccurrence.appointmentOccurrenceId,
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
    fun `deletes occurrences with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceCancelRequest(cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId)
      val cancelled = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.cancelAppointmentOccurrenceIds(
        appointmentSeries.appointmentSeriesId,
        appointmentOccurrence.appointmentOccurrenceId,
        ids,
        request,
        cancelled,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      response.occurrences.filter { ids.contains(it.id) } hasSize 0
      response.occurrences.filterNot { ids.contains(it.id) } hasSize 1

      verify(service).cancelAppointmentOccurrences(
        appointmentSeries,
        appointmentOccurrence.appointmentOccurrenceId,
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
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceCancelRequest(cancellationReasonId = appointmentDeletedReason.appointmentCancellationReasonId)
      val startTimeInMs = System.currentTimeMillis()
      service.cancelAppointmentOccurrenceIds(
        appointmentSeries.appointmentSeriesId,
        appointmentOccurrence.appointmentOccurrenceId,
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

  @Nested
  @DisplayName("instance count")
  inner class CancelAppointmentOccurrenceInstanceCount {
    @Test
    fun `this occurrence`() {
      service.getCancelInstancesCount(applyToThis) isEqualTo applyToThis.flatMap { it.allocations() }.size
    }

    @Test
    fun `this and all future occurrences`() {
      service.getCancelInstancesCount(applyToThisAndAllFuture) isEqualTo applyToThisAndAllFuture.flatMap { it.allocations() }.size
    }

    @Test
    fun `all future occurrences`() {
      service.getCancelInstancesCount(applyToAllFuture) isEqualTo applyToAllFuture.flatMap { it.allocations() }.size
    }
  }
}
