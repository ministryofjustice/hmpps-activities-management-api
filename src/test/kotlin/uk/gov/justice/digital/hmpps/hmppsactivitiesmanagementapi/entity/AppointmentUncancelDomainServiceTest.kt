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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreatedInErrorReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentUncancelDomainServiceTest {
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
  private val existingCancelledReason = AppointmentCancellationReason(1L, "Cancelled", false)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4, cancellationReason = existingCancelledReason, cancelledBy = "CANCELLED.USER", cancelledTime = LocalDateTime.now().plusDays(1))
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThis = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "uncancel", true)
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "uncancel", true)
  private val applyToAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "uncancel", true)

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
  @DisplayName("uncancel by ids - used by async uncancel appointments job")
  inner class CancelAppointmentIds {
    @Test
    fun `uncancels appointments with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentUncancelRequest()
      val updatedTime = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.uncancelAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
        ids,
        request,
        updatedTime,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      response.appointments.filter { ids.contains(it.id) }.map { it.isCancelled() }.distinct().single() isEqualTo false
      response.appointments.filterNot { ids.contains(it.id) }.map { it.isCancelled() }.distinct().single() isEqualTo true

      verify(service).uncancelAppointments(
        appointmentSeries,
        appointment.appointmentId,
        applyToThisAndAllFuture.toSet(),
        request,
        updatedTime,
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
    fun `track uncancelled custom event using supplied counts and start time`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentUncancelRequest()
      val startTimeInMs = System.currentTimeMillis()
      service.uncancelAppointmentIds(
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

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_UNCANCELLED.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryMetricsMap.firstValue) {
        this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 3.0
        this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 10.0
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
      }

      verifyNoInteractions(auditService)
    }
  }

  @Test
  fun `appointment instance un-cancelled sync events raised on appointment update when appointment is un-cancelled`() {
    val appointmentSeries = appointmentSeriesEntity(
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L),
      numberOfAppointments = 4,
      frequency = AppointmentFrequency.DAILY,
      cancellationReason = existingCancelledReason,
      cancelledBy = "CANCELLED.USER",
      cancelledTime = LocalDateTime.now().plusDays(1),
    )
    val appointment = appointmentSeries.appointments()[0]
    val applyToThis = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "uncancel", true)

    val ids = applyToThis.map { it.appointmentId }.toSet()
    val request = AppointmentUncancelRequest()
    val startTimeInMs = System.currentTimeMillis()

    ids.size isEqualTo 4

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.uncancelAppointmentIds(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      ids,
      request,
      LocalDateTime.now(),
      "TEST.USER",
      4,
      12,
      startTimeInMs,
    )

    verify(outboundEventsService, times(12)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED), any())
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance uncancelled with ApplyTo THIS_AND_ALL_FUTURE_APPOINTMENTS un-cancels the series`() {
    val appointmentSeriesMock: AppointmentSeries = mock()
    whenever(appointmentSeriesMock.prisonCode).thenReturn("ABC")
    whenever(appointmentSeriesMock.categoryCode).thenReturn("ABC")
    whenever(appointmentSeriesMock.createdTime).thenReturn(LocalDateTime.now())
    whenever(appointmentSeriesMock.createdBy).thenReturn("TEST")

    var appointmentList = mutableListOf<Appointment>()
    val iterator = AppointmentSeriesScheduleIterator(LocalDate.now(), AppointmentFrequency.DAILY, 4)
    iterator.withIndex().forEach {
      appointmentList.add(appointmentEntity(appointmentSeriesMock, 1 * (it.index + 1L), it.index + 1, it.value, LocalTime.now(), LocalDateTime.now(), "updatedBy", prisonerNumberToBookingIdMap, cancellationReason = existingCancelledReason, cancelledBy = "CANCELLED.USER", cancelledTime = LocalDateTime.now().plusDays(1)))
    }

    whenever(appointmentSeriesMock.appointments()).thenReturn(appointmentList)
    whenever(appointmentSeriesMock.applyToAppointments(any(), any(), any(), any())).thenReturn(appointmentList)

    val appointment = appointmentSeriesMock.appointments()[0]
    val applyToThis = appointmentSeriesMock.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "uncancel", true)

    val ids = applyToThis.map { it.appointmentId }.toSet()
    val request = AppointmentUncancelRequest(applyTo = ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS)
    val startTimeInMs = System.currentTimeMillis()

    ids.size isEqualTo 4

    whenever(appointmentSeriesRepository.findById(appointmentSeriesMock.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeriesMock))

    val now = LocalDateTime.now()

    service.uncancelAppointmentIds(
      appointmentSeriesMock.appointmentSeriesId,
      appointment.appointmentId,
      ids,
      request,
      now,
      "TEST.USER",
      4,
      12,
      startTimeInMs,
    )

    verify(outboundEventsService, times(12)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED), any())
    verifyNoMoreInteractions(outboundEventsService)

    verify(appointmentSeriesMock, times(1)).uncancel(now, "TEST.USER")
  }

  @Test
  fun `appointment instance un-cancelled with ApplyTo ALL_FUTURE_APPOINTMENTS un-cancels the series`() {
    val appointmentSeriesMock: AppointmentSeries = mock()
    whenever(appointmentSeriesMock.prisonCode).thenReturn("ABC")
    whenever(appointmentSeriesMock.categoryCode).thenReturn("ABC")
    whenever(appointmentSeriesMock.createdTime).thenReturn(LocalDateTime.now())
    whenever(appointmentSeriesMock.createdBy).thenReturn("TEST")

    var appointmentList = mutableListOf<Appointment>()
    val iterator = AppointmentSeriesScheduleIterator(LocalDate.now(), AppointmentFrequency.DAILY, 4)
    iterator.withIndex().forEach {
      appointmentList.add(appointmentEntity(appointmentSeriesMock, 1 * (it.index + 1L), it.index + 1, it.value, LocalTime.now(), LocalDateTime.now(), "updatedBy", prisonerNumberToBookingIdMap, cancellationReason = existingCancelledReason, cancelledBy = "CANCELLED.USER", cancelledTime = LocalDateTime.now().plusDays(1)))
    }

    whenever(appointmentSeriesMock.appointments()).thenReturn(appointmentList)
    whenever(appointmentSeriesMock.applyToAppointments(any(), any(), any(), any())).thenReturn(appointmentList)

    val appointment = appointmentSeriesMock.appointments()[0]
    val applyToThis = appointmentSeriesMock.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "uncancel", true)

    val ids = applyToThis.map { it.appointmentId }.toSet()
    val request = AppointmentUncancelRequest(applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)
    val startTimeInMs = System.currentTimeMillis()

    ids.size isEqualTo 4

    whenever(appointmentSeriesRepository.findById(appointmentSeriesMock.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeriesMock))

    val now = LocalDateTime.now()

    service.uncancelAppointmentIds(
      appointmentSeriesMock.appointmentSeriesId,
      appointment.appointmentId,
      ids,
      request,
      now,
      "TEST.USER",
      4,
      12,
      startTimeInMs,
    )

    verify(outboundEventsService, times(12)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED), any())
    verifyNoMoreInteractions(outboundEventsService)

    verify(appointmentSeriesMock, times(1)).uncancel(now, "TEST.USER")
  }

  @Test
  fun `appointment instance un-cancelled with ApplyTo THIS_APPOINTMENT un-cancels the series`() {
    val appointmentSeriesMock: AppointmentSeries = mock()
    whenever(appointmentSeriesMock.prisonCode).thenReturn("ABC")
    whenever(appointmentSeriesMock.categoryCode).thenReturn("ABC")
    whenever(appointmentSeriesMock.createdTime).thenReturn(LocalDateTime.now())
    whenever(appointmentSeriesMock.createdBy).thenReturn("TEST")

    var appointmentList = mutableListOf<Appointment>()
    val iterator = AppointmentSeriesScheduleIterator(LocalDate.now(), AppointmentFrequency.DAILY, 4)
    iterator.withIndex().forEach {
      appointmentList.add(appointmentEntity(appointmentSeriesMock, 1 * (it.index + 1L), it.index + 1, it.value, LocalTime.now(), LocalDateTime.now(), "updatedBy", prisonerNumberToBookingIdMap, cancellationReason = existingCancelledReason, cancelledBy = "CANCELLED.USER", cancelledTime = LocalDateTime.now().plusDays(1)))
    }

    whenever(appointmentSeriesMock.appointments()).thenReturn(appointmentList)
    whenever(appointmentSeriesMock.applyToAppointments(any(), any(), any(), any())).thenReturn(appointmentList)

    val appointment = appointmentSeriesMock.appointments()[0]
    val applyToThis = appointmentSeriesMock.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "", false)

    val ids = applyToThis.map { it.appointmentId }.toSet()
    val request = AppointmentUncancelRequest(applyTo = ApplyTo.THIS_APPOINTMENT)
    val startTimeInMs = System.currentTimeMillis()

    ids.size isEqualTo 4

    whenever(appointmentSeriesRepository.findById(appointmentSeriesMock.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeriesMock))

    val now = LocalDateTime.now()

    service.uncancelAppointmentIds(
      appointmentSeriesMock.appointmentSeriesId,
      appointment.appointmentId,
      ids,
      request,
      now,
      "TEST.USER",
      4,
      12,
      startTimeInMs,
    )

    verify(outboundEventsService, times(12)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED), any())
    verifyNoMoreInteractions(outboundEventsService)

    verify(appointmentSeriesMock, times(0)).uncancel(now, "TEST.USER")
  }

  @Nested
  @DisplayName("instance count")
  inner class CancelInstanceCount {
    @Test
    fun `this appointment`() {
      service.getUncancelInstancesCount(applyToThis) isEqualTo applyToThis.flatMap { it.attendees() }.size
    }

    @Test
    fun `this and all future appointments`() {
      service.getUncancelInstancesCount(applyToThisAndAllFuture) isEqualTo applyToThisAndAllFuture.flatMap { it.attendees() }.size
    }

    @Test
    fun `all future appointments`() {
      service.getUncancelInstancesCount(applyToAllFuture) isEqualTo applyToAllFuture.flatMap { it.attendees() }.size
    }
  }
}
