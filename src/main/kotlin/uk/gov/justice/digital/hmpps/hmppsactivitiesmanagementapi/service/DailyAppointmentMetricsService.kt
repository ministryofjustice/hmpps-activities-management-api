package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_RECORDED_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_RECORDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_RECORDED_NOT_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CANCELLED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DELETED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class DailyAppointmentMetricsService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun generateAppointmentMetrics(prisonCode: String, categoryCode: String, date: LocalDate) {
    val propertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to prisonCode,
      CATEGORY_CODE_PROPERTY_KEY to categoryCode,
    )

    val metricsMap = appointmentRepository.findByPrisonCodeAndCategoryCodeAndDate(prisonCode, categoryCode, date).let { appointments ->
      val activeAppointments = appointments.filterNot { it.isCancelled() || it.isDeleted }
      mutableMapOf(
        APPOINTMENT_COUNT_METRIC_KEY to activeAppointments.size.toDouble(),
        APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to activeAppointments.countAttendees(),
        APPOINTMENT_SERIES_COUNT_METRIC_KEY to activeAppointments.countUniqueRepeatingAppointmentSeries(),
        APPOINTMENT_SET_COUNT_METRIC_KEY to activeAppointments.countUniqueAppointmentSets(),
        CANCELLED_APPOINTMENT_COUNT_METRIC_KEY to appointments.countCancelledAppointments(),
        DELETED_APPOINTMENT_COUNT_METRIC_KEY to appointments.countDeletedAppointments(),
      )
    }.also {
      appointmentAttendeeRepository.findByPrisonCodeAndCategoryAndRecordedDate(prisonCode, categoryCode, date).let { attendance ->
        it[ATTENDANCE_RECORDED_COUNT_METRIC_KEY] = attendance.size.toDouble()
        it[ATTENDANCE_RECORDED_ATTENDED_COUNT_METRIC_KEY] = attendance.countAttended()
        it[ATTENDANCE_RECORDED_NOT_ATTENDED_COUNT_METRIC_KEY] = attendance.countNotAttended()
      }
    }

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value, propertiesMap, metricsMap)
  }

  private fun List<Appointment>.countAttendees() = this.flatMap { it.attendees() }.size.toDouble()

  private fun List<AppointmentAttendee>.countAttended() = this.filter { it.attended == true }.size.toDouble()

  private fun List<AppointmentAttendee>.countNotAttended() = this.filter { it.attended == false }.size.toDouble()

  private fun List<Appointment>.countUniqueRepeatingAppointmentSeries() =
    this.map { it.appointmentSeries }.filter { (it.schedule?.numberOfAppointments ?: 0) > 1 }.map { it.appointmentSeriesId }.distinct().size.toDouble()

  private fun List<Appointment>.countUniqueAppointmentSets() =
    this.mapNotNull { it.appointmentSeries.appointmentSet }.map { it.appointmentSetId }.distinct().size.toDouble()

  private fun List<Appointment>.countCancelledAppointments() = this.filter { it.isCancelled() }.size.toDouble()

  private fun List<Appointment>.countDeletedAppointments() = this.filter { it.isDeleted }.size.toDouble()
}
