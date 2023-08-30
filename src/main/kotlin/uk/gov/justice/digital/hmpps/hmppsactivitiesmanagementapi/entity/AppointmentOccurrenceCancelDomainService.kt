package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
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
    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    val occurrencesToCancel = appointment.occurrences().filter { occurrenceIdsToCancel.contains(it.appointmentOccurrenceId) }
    return cancelAppointmentOccurrences(appointment, appointmentOccurrenceId, occurrencesToCancel.toSet(), request, cancelled, cancelledBy, cancelOccurrencesCount, cancelInstancesCount, startTimeInMs, true)
  }

  fun cancelAppointmentOccurrences(
    appointment: Appointment,
    appointmentOccurrenceId: Long,
    occurrencesToCancel: Set<AppointmentOccurrence>,
    request: AppointmentOccurrenceCancelRequest,
    cancelled: LocalDateTime,
    cancelledBy: String,
    cancelOccurrencesCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
  ): AppointmentModel {
    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    occurrencesToCancel.forEach {
      it.cancelled = cancelled
      it.cancellationReason = cancellationReason
      it.cancelledBy = cancelledBy
      it.deleted = cancellationReason.isDelete
    }

    val cancelledAppointment = appointmentRepository.saveAndFlush(appointment)

    if (trackEvent) {
      val customEventName = if (cancellationReason.isDelete) TelemetryEvent.APPOINTMENT_DELETED.value else TelemetryEvent.APPOINTMENT_CANCELLED.value
      val telemetryPropertiesMap = request.toTelemetryPropertiesMap(cancelledBy, appointment.prisonCode, appointment.appointmentId, appointmentOccurrenceId)
      val telemetryMetricsMap = mapOf(
        APPOINTMENT_COUNT_METRIC_KEY to cancelOccurrencesCount.toDouble(),
        APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to cancelInstancesCount.toDouble(),
        EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
      )
      telemetryClient.trackEvent(customEventName, telemetryPropertiesMap, telemetryMetricsMap)
    }

    return cancelledAppointment.toModel()
  }

  fun getCancelInstancesCount(
    occurrencesToCancel: Collection<AppointmentOccurrence>,
  ) = occurrencesToCancel.flatMap { it.allocations() }.size
}