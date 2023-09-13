package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries as AppointmentSeriesModel

@Service
@Transactional
class AppointmentCancelDomainService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  fun cancelAppointmentIds(
    appointmentSeriesId: Long,
    appointmentId: Long,
    appointmentIdsToCancel: Set<Long>,
    request: AppointmentCancelRequest,
    cancelled: LocalDateTime,
    cancelledBy: String,
    cancelAppointmentsCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
  ): AppointmentSeriesModel {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    val appointmentsToCancel = appointmentSeries.appointments().filter { appointmentIdsToCancel.contains(it.appointmentId) }
    return cancelAppointments(appointmentSeries, appointmentId, appointmentsToCancel.toSet(), request, cancelled, cancelledBy, cancelAppointmentsCount, cancelInstancesCount, startTimeInMs, true, false)
  }

  fun cancelAppointments(
    appointmentSeries: AppointmentSeries,
    appointmentId: Long,
    appointmentsToCancel: Set<Appointment>,
    request: AppointmentCancelRequest,
    cancelled: LocalDateTime,
    cancelledBy: String,
    cancelAppointmentsCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
    auditEvent: Boolean,
  ): AppointmentSeriesModel {
    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    appointmentsToCancel.forEach {
      it.cancelledTime = cancelled
      it.cancellationReason = cancellationReason
      it.cancelledBy = cancelledBy
      it.isDeleted = cancellationReason.isDelete
    }

    val cancelledAppointment = appointmentSeriesRepository.saveAndFlush(appointmentSeries)

    if (trackEvent) {
      val customEventName = if (cancellationReason.isDelete) TelemetryEvent.APPOINTMENT_DELETED.value else TelemetryEvent.APPOINTMENT_CANCELLED.value
      val telemetryPropertiesMap = request.toTelemetryPropertiesMap(cancelledBy, appointmentSeries.prisonCode, appointmentSeries.appointmentSeriesId, appointmentId)
      val telemetryMetricsMap = mapOf(
        APPOINTMENT_COUNT_METRIC_KEY to cancelAppointmentsCount.toDouble(),
        APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to cancelInstancesCount.toDouble(),
        EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
      )
      telemetryClient.trackEvent(customEventName, telemetryPropertiesMap, telemetryMetricsMap)
    }

    if (auditEvent) {
      writeAuditEvent(appointmentId, request, appointmentSeries, cancellationReason.isDelete)
    }

    return cancelledAppointment.toModel()
  }

  fun getCancelInstancesCount(
    appointmentsToCancel: Collection<Appointment>,
  ) = appointmentsToCancel.flatMap { it.attendees() }.size

  private fun writeAuditEvent(
    appointmentId: Long,
    request: AppointmentCancelRequest,
    appointmentSeries: AppointmentSeries,
    isDelete: Boolean,
  ) {
    if (isDelete) {
      writeAppointmentDeletedAuditEvent(appointmentId, request, appointmentSeries)
    } else {
      writeAppointmentCancelledAuditEvent(appointmentId, request, appointmentSeries)
    }
  }
  private fun writeAppointmentCancelledAuditEvent(
    appointmentId: Long,
    request: AppointmentCancelRequest,
    appointmentSeries: AppointmentSeries,
  ) {
    auditService.logEvent(
      AppointmentCancelledEvent(
        appointmentSeriesId = appointmentSeries.appointmentSeriesId,
        appointmentId = appointmentId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = LocalDateTime.now(),
      ),
    )
  }

  private fun writeAppointmentDeletedAuditEvent(
    appointmentId: Long,
    request: AppointmentCancelRequest,
    appointmentSeries: AppointmentSeries,
  ) {
    auditService.logEvent(
      AppointmentDeletedEvent(
        appointmentSeriesId = appointmentSeries.appointmentSeriesId,
        appointmentId = appointmentId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = LocalDateTime.now(),
      ),
    )
  }
}
