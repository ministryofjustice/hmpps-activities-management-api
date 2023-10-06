package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentAttendanceService(
  private val appointmentRepository: AppointmentRepository,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  fun markAttendance(appointmentId: Long, request: AppointmentAttendanceRequest, principal: Principal): Appointment {
    val startTimeInMs = System.currentTimeMillis()
    val attendanceRecordedTime = LocalDateTime.now()
    val attendanceRecordedBy = principal.name

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.prisonCode)

    appointment.markPrisonersAttended(request.attendedPrisonNumbers, attendanceRecordedTime, attendanceRecordedBy)
    appointment.markPrisonersNonAttended(request.nonAttendedPrisonNumbers, attendanceRecordedTime, attendanceRecordedBy)

    val telemetryPropertiesMap = request.toTelemetryPropertiesMap(attendanceRecordedBy, appointment.prisonCode, appointmentId)
    val telemetryMetricsMap = request.toTelemetryMetricsMap()
    telemetryMetricsMap[EVENT_TIME_MS_METRIC_KEY] = (System.currentTimeMillis() - startTimeInMs).toDouble()
    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value, telemetryPropertiesMap, telemetryMetricsMap)

    // writeAppointmentUpdatedAuditRecord(appointmentId, request, appointmentSeries, updatedAppointment)

    return appointment.toModel()
  }
}
