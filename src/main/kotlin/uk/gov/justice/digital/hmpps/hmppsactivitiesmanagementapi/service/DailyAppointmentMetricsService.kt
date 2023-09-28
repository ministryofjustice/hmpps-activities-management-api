package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CANCELLED_APPOINTMENT_COUNT_METRIC_KEY

@Service
class DailyAppointmentMetricsService {

  fun generateAppointmentMetrics(metricsMap: MutableMap<String, Double>, appointments: List<Appointment>) {
    appointments.forEach {
      if (!it.isCancelled() && !it.isDeleted) {
        incrementMetric(metricsMap, APPOINTMENT_COUNT_METRIC_KEY)

        it.attendees().forEach { _ -> incrementMetric(metricsMap, APPOINTMENT_INSTANCE_COUNT_METRIC_KEY) }

        if ((it.appointmentSeries.schedule?.numberOfAppointments ?: 0) > 1) {
          incrementMetric(metricsMap, APPOINTMENT_SERIES_COUNT_METRIC_KEY)
        }

        if (it.appointmentSeries.appointmentSet != null) {
          incrementMetric(metricsMap, APPOINTMENT_SET_COUNT_METRIC_KEY)
        }
      }

      if (it.isCancelled() && !it.isDeleted) {
        incrementMetric(metricsMap, CANCELLED_APPOINTMENT_COUNT_METRIC_KEY)
      }
    }
  }

  private fun incrementMetric(metricsMap: MutableMap<String, Double>, metricKey: String, increment: Int = 1) {
    metricsMap[metricKey] = ((metricsMap[metricKey] ?: 0.0) + increment)
  }
}
