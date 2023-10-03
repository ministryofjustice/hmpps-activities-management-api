package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_COUNT_METRIC_KEY
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
  private val telemetryClient: TelemetryClient,
) {
  fun generateAppointmentMetrics(prisonCode: String, categoryCode: String, date: LocalDate) {
    val propertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to prisonCode,
      CATEGORY_CODE_PROPERTY_KEY to categoryCode,
    )

    val metricsMap = mutableMapOf(
      APPOINTMENT_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_SERIES_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_SET_COUNT_METRIC_KEY to 0.0,
      CANCELLED_APPOINTMENT_COUNT_METRIC_KEY to 0.0,
      DELETED_APPOINTMENT_COUNT_METRIC_KEY to 0.0,
    )

    appointmentRepository.findByPrisonCodeAndCategoryCodeAndDate(prisonCode, categoryCode, date).apply {
      this.generateAppointmentMetrics(metricsMap)
    }

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value, propertiesMap, metricsMap)
  }

  private fun List<Appointment>.generateAppointmentMetrics(metricsMap: MutableMap<String, Double>) {
    this.forEach {
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

      if (it.isDeleted) {
        incrementMetric(metricsMap, DELETED_APPOINTMENT_COUNT_METRIC_KEY)
      }
    }
  }

  private fun incrementMetric(metricsMap: MutableMap<String, Double>, metricKey: String, increment: Int = 1) {
    metricsMap[metricKey] = ((metricsMap[metricKey] ?: 0.0) + increment)
  }
}
