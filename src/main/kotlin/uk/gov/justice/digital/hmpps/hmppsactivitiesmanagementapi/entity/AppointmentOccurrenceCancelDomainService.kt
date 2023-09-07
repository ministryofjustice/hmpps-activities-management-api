package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
@Transactional
class AppointmentOccurrenceCancelDomainService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  fun cancelAppointmentOccurrenceIds(
    appointmentId: Long,
    appointmentOccurrenceId: Long,
    occurrenceIdsToCancel: Set<Long>,
    request: AppointmentOccurrenceCancelRequest,
    cancelled: LocalDateTime,
    cancelledBy: String,
    cancelOccurrencesCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
  ): AppointmentModel {
    val appointmentSeries = appointmentRepository.findOrThrowNotFound(appointmentId)
    val occurrencesToCancel = appointmentSeries.occurrences().filter { occurrenceIdsToCancel.contains(it.appointmentOccurrenceId) }
    return cancelAppointmentOccurrences(appointmentSeries, appointmentOccurrenceId, occurrencesToCancel.toSet(), request, cancelled, cancelledBy, cancelOccurrencesCount, cancelInstancesCount, startTimeInMs, true, false)
  }

  fun cancelAppointmentOccurrences(
    appointmentSeries: AppointmentSeries,
    appointmentOccurrenceId: Long,
    occurrencesToCancel: Set<AppointmentOccurrence>,
    request: AppointmentOccurrenceCancelRequest,
    cancelled: LocalDateTime,
    cancelledBy: String,
    cancelOccurrencesCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
    auditEvent: Boolean,
  ): AppointmentModel {
    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    occurrencesToCancel.forEach {
      it.cancelled = cancelled
      it.cancellationReason = cancellationReason
      it.cancelledBy = cancelledBy
      it.deleted = cancellationReason.isDelete
    }

    val cancelledAppointment = appointmentRepository.saveAndFlush(appointmentSeries)

    if (trackEvent) {
      val customEventName = if (cancellationReason.isDelete) TelemetryEvent.APPOINTMENT_DELETED.value else TelemetryEvent.APPOINTMENT_CANCELLED.value
      val telemetryPropertiesMap = request.toTelemetryPropertiesMap(cancelledBy, appointmentSeries.prisonCode, appointmentSeries.appointmentId, appointmentOccurrenceId)
      val telemetryMetricsMap = mapOf(
        APPOINTMENT_COUNT_METRIC_KEY to cancelOccurrencesCount.toDouble(),
        APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to cancelInstancesCount.toDouble(),
        EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
      )
      telemetryClient.trackEvent(customEventName, telemetryPropertiesMap, telemetryMetricsMap)
    }

    if (auditEvent) {
      writeAuditEvent(appointmentOccurrenceId, request, appointmentSeries, cancellationReason.isDelete)
    }

    return cancelledAppointment.toModel()
  }

  fun getCancelInstancesCount(
    occurrencesToCancel: Collection<AppointmentOccurrence>,
  ) = occurrencesToCancel.flatMap { it.allocations() }.size

  private fun writeAuditEvent(
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointmentSeries: AppointmentSeries,
    isDelete: Boolean,
  ) {
    if (isDelete) {
      writeAppointmentDeletedAuditEvent(appointmentOccurrenceId, request, appointmentSeries)
    } else {
      writeAppointmentCancelledAuditEvent(appointmentOccurrenceId, request, appointmentSeries)
    }
  }
  private fun writeAppointmentCancelledAuditEvent(
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointmentSeries: AppointmentSeries,
  ) {
    auditService.logEvent(
      AppointmentCancelledEvent(
        appointmentId = appointmentSeries.appointmentId,
        appointmentOccurrenceId = appointmentOccurrenceId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = LocalDateTime.now(),
      ),
    )
  }

  private fun writeAppointmentDeletedAuditEvent(
    appointmentOccurrenceId: Long,
    request: AppointmentOccurrenceCancelRequest,
    appointmentSeries: AppointmentSeries,
  ) {
    auditService.logEvent(
      AppointmentDeletedEvent(
        appointmentId = appointmentSeries.appointmentId,
        appointmentOccurrenceId = appointmentOccurrenceId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = LocalDateTime.now(),
      ),
    )
  }
}
