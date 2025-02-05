package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AppointmentCancellationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentUncancelledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
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
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) {
  fun cancelAppointmentIds(
    appointmentSeriesId: Long,
    appointmentId: Long,
    appointmentIdsToCancel: Set<Long>,
    request: AppointmentCancelRequest,
    cancelledTime: LocalDateTime,
    cancelledBy: String,
    cancelAppointmentsCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
  ): AppointmentSeriesModel {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    val appointmentsToCancel = appointmentSeries.appointments().filter { appointmentIdsToCancel.contains(it.appointmentId) }
    return cancelAppointments(
      appointmentSeries,
      appointmentId,
      appointmentsToCancel.toSet(),
      request,
      cancelledTime,
      cancelledBy,
      cancelAppointmentsCount,
      cancelInstancesCount,
      startTimeInMs,
      trackEvent = true,
      auditEvent = false,
    )
  }

  fun uncancelAppointmentIds(
    appointmentSeriesId: Long,
    appointmentId: Long,
    appointmentIdsToUncancel: Set<Long>,
    request: AppointmentUncancelRequest,
    updatedTime: LocalDateTime,
    updatedBy: String,
    uncancelAppointmentsCount: Int,
    uncancelInstancesCount: Int,
    startTimeInMs: Long,
  ): AppointmentSeriesModel {
    val appointmentSeries = appointmentSeriesRepository.findOrThrowNotFound(appointmentSeriesId)
    val appointmentsToUncancel = appointmentSeries.appointments().filter { appointmentIdsToUncancel.contains(it.appointmentId) }
    return uncancelAppointments(
      appointmentSeries,
      appointmentId,
      appointmentsToUncancel.toSet(),
      request,
      updatedTime,
      updatedBy,
      uncancelAppointmentsCount,
      uncancelInstancesCount,
      startTimeInMs,
      trackEvent = true,
      auditEvent = false,
    )
  }

  fun uncancelAppointments(
    appointmentSeries: AppointmentSeries,
    appointmentId: Long,
    appointmentsToUncancel: Set<Appointment>,
    request: AppointmentUncancelRequest,
    updatedTime: LocalDateTime,
    updatedBy: String,
    uncancelAppointmentsCount: Int,
    uncancelInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
    auditEvent: Boolean,
  ): AppointmentSeriesModel {
    transactionHandler.newSpringTransaction {
      appointmentsToUncancel.forEach {
        it.uncancel(updatedBy, updatedTime)
      }
      if (request.applyTo == ApplyTo.ALL_FUTURE_APPOINTMENTS || request.applyTo == ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS) {
        appointmentSeries.uncancel(updatedTime, updatedBy)
      }
    }.let {
      publishUncancelSyncEvent(appointmentsToUncancel)

      if (trackEvent) {
        sendTelemetryUncancelEvent(
          request,
          updatedBy,
          appointmentSeries,
          appointmentId,
          uncancelAppointmentsCount,
          uncancelInstancesCount,
          startTimeInMs,
        )
      }

      if (auditEvent) {
        writeAppointmentUncancelledAuditEvent(
          appointmentId,
          request,
          appointmentSeries,
          updatedTime,
          updatedBy,
        )
      }

      return appointmentSeries.toModel()
    }
  }

  fun cancelAppointments(
    appointmentSeries: AppointmentSeries,
    appointmentId: Long,
    appointmentsToCancel: Set<Appointment>,
    request: AppointmentCancelRequest,
    cancelledTime: LocalDateTime,
    cancelledBy: String,
    cancelAppointmentsCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
    trackEvent: Boolean,
    auditEvent: Boolean,
  ): AppointmentSeriesModel {
    val cancellationReason = appointmentCancellationReasonRepository.findOrThrowNotFound(request.cancellationReasonId)

    transactionHandler.newSpringTransaction {
      appointmentsToCancel.forEach {
        it.cancel(cancelledTime, cancellationReason, cancelledBy)
      }
      if (request.applyTo == ApplyTo.ALL_FUTURE_APPOINTMENTS || request.applyTo == ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS) {
        val firstAppointment = appointmentsToCancel.minWith(Comparator.comparing { it.startDateTime() })
        appointmentSeries.cancel(cancelledTime, cancelledBy, firstAppointment.startDate, firstAppointment.startTime)
      }
    }.let {
      publishSyncEvent(appointmentsToCancel, cancellationReason)

      if (trackEvent) {
        sendTelemetryEvent(
          cancellationReason,
          request,
          cancelledBy,
          appointmentSeries,
          appointmentId,
          cancelAppointmentsCount,
          cancelInstancesCount,
          startTimeInMs,
        )
      }

      if (auditEvent) {
        writeAuditEvent(
          appointmentId,
          request,
          appointmentSeries,
          cancelledTime,
          cancelledBy,
          cancellationReason.isDelete,
        )
      }

      return appointmentSeries.toModel()
    }
  }

  fun getCancelInstancesCount(
    appointmentsToCancel: Collection<Appointment>,
  ) = appointmentsToCancel.flatMap { it.attendees() }.size

  fun getUncancelInstancesCount(
    appointmentsToUncancel: Collection<Appointment>,
  ) = appointmentsToUncancel.flatMap { it.attendees() }.size

  private fun publishSyncEvent(
    appointmentsToCancel: Set<Appointment>,
    cancellationReason: AppointmentCancellationReason,
  ) {
    val syncEvent = if (cancellationReason.isDelete) {
      OutboundEvent.APPOINTMENT_INSTANCE_DELETED
    } else {
      OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
    }
    appointmentsToCancel.forEach {
      it.attendees().forEach { attendee ->
        outboundEventsService.send(syncEvent, attendee.appointmentAttendeeId)
      }
    }
  }

  private fun publishUncancelSyncEvent(
    appointmentsToUncancel: Set<Appointment>,
  ) {
    appointmentsToUncancel.forEach {
      it.attendees().forEach { attendee ->
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED, attendee.appointmentAttendeeId)
      }
    }
  }

  private fun sendTelemetryEvent(
    cancellationReason: AppointmentCancellationReason,
    request: AppointmentCancelRequest,
    cancelledBy: String,
    appointmentSeries: AppointmentSeries,
    appointmentId: Long,
    cancelAppointmentsCount: Int,
    cancelInstancesCount: Int,
    startTimeInMs: Long,
  ) {
    val customEventName =
      if (cancellationReason.isDelete) TelemetryEvent.APPOINTMENT_DELETED.value else TelemetryEvent.APPOINTMENT_CANCELLED.value
    val telemetryPropertiesMap = request.toTelemetryPropertiesMap(
      cancelledBy,
      appointmentSeries.prisonCode,
      appointmentSeries.appointmentSeriesId,
      appointmentId,
    )
    val telemetryMetricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to cancelAppointmentsCount.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to cancelInstancesCount.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )
    telemetryClient.trackEvent(customEventName, telemetryPropertiesMap, telemetryMetricsMap)
  }

  private fun sendTelemetryUncancelEvent(
    request: AppointmentUncancelRequest,
    updatedBy: String,
    appointmentSeries: AppointmentSeries,
    appointmentId: Long,
    uncancelAppointmentsCount: Int,
    uncancelInstancesCount: Int,
    startTimeInMs: Long,
  ) {
    val telemetryPropertiesMap = request.toTelemetryPropertiesMap(
      updatedBy,
      appointmentSeries.prisonCode,
      appointmentSeries.appointmentSeriesId,
      appointmentId,
    )
    val telemetryMetricsMap = mapOf(
      APPOINTMENT_COUNT_METRIC_KEY to uncancelAppointmentsCount.toDouble(),
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to uncancelInstancesCount.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )
    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_UNCANCELLED.value, telemetryPropertiesMap, telemetryMetricsMap)
  }

  private fun writeAuditEvent(
    appointmentId: Long,
    request: AppointmentCancelRequest,
    appointmentSeries: AppointmentSeries,
    cancelledTime: LocalDateTime,
    cancelledBy: String,
    isDelete: Boolean,
  ) {
    if (isDelete) {
      writeAppointmentDeletedAuditEvent(appointmentId, request, appointmentSeries, cancelledTime, cancelledBy)
    } else {
      writeAppointmentCancelledAuditEvent(appointmentId, request, appointmentSeries, cancelledTime, cancelledBy)
    }
  }
  private fun writeAppointmentCancelledAuditEvent(
    appointmentId: Long,
    request: AppointmentCancelRequest,
    appointmentSeries: AppointmentSeries,
    cancelledTime: LocalDateTime,
    cancelledBy: String,
  ) {
    auditService.logEvent(
      AppointmentCancelledEvent(
        appointmentSeriesId = appointmentSeries.appointmentSeriesId,
        appointmentId = appointmentId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = cancelledTime,
        createdBy = cancelledBy,
      ),
    )
  }

  private fun writeAppointmentDeletedAuditEvent(
    appointmentId: Long,
    request: AppointmentCancelRequest,
    appointmentSeries: AppointmentSeries,
    cancelledTime: LocalDateTime,
    cancelledBy: String,
  ) {
    auditService.logEvent(
      AppointmentDeletedEvent(
        appointmentSeriesId = appointmentSeries.appointmentSeriesId,
        appointmentId = appointmentId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = cancelledTime,
        createdBy = cancelledBy,
      ),
    )
  }

  private fun writeAppointmentUncancelledAuditEvent(
    appointmentId: Long,
    request: AppointmentUncancelRequest,
    appointmentSeries: AppointmentSeries,
    updatedTime: LocalDateTime,
    updatedBy: String,
  ) {
    auditService.logEvent(
      AppointmentUncancelledEvent(
        appointmentSeriesId = appointmentSeries.appointmentSeriesId,
        appointmentId = appointmentId,
        prisonCode = appointmentSeries.prisonCode,
        applyTo = request.applyTo,
        createdAt = updatedTime,
        createdBy = updatedBy,
      ),
    )
  }
}
