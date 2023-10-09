package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendanceMarkedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap

@Service
class AppointmentTransactionalEventListener(
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAppointmentAttendanceMarkedEvent(event: AppointmentAttendanceMarkedEvent) {
    listOf(
      if (event.attendedPrisonNumbers.isEmpty()) "" else "attendance for '${event.attendedPrisonNumbers.joinToString("', '")}'",
      if (event.nonAttendedPrisonNumbers.isEmpty()) "" else "non attendance for '${event.attendedPrisonNumbers.joinToString("', '")}'",
    )

    log.info(
      """
      User with username '${event.attendanceRecordedBy}' 
      marked attendance for prison numbers '${event.attendedPrisonNumbers.joinToString("', '")}' 
      and non-attendance for prison numbers '${event.nonAttendedPrisonNumbers.joinToString("', '")}' 
      on attendee records for appointment with id '${event.appointmentId}'. 
      This changed the attendance for prison numbers '${event.attendanceChangedPrisonNumbers.joinToString("', '")}'.
      """.trimIndent(),
    )

    val telemetryPropertiesMap = event.toTelemetryPropertiesMap()
    val telemetryMetricsMap = event.toTelemetryMetricsMap()
    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value, telemetryPropertiesMap, telemetryMetricsMap)
  }
}
