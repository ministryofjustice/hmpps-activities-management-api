package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.DailyAppointmentMetricsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduleReasonEventType
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
import kotlin.system.measureTimeMillis

@Component
class AppointmentMetricsJob(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val appointmentRepository: AppointmentRepository,
  private val prisonApiClient: PrisonApiApplicationClient,
  private val dailyAppointmentMetricsService: DailyAppointmentMetricsService,
  private val telemetryClient: TelemetryClient,
  private val jobRunner: SafeJobRunner,

) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    jobRunner.runJob(
      JobDefinition(JobType.APPOINTMENTS_METRICS) {
        log.info("Generating daily appointments metrics")

        val elapsed = measureTimeMillis {
          val allPrisonCodes = rolloutPrisonRepository.findAll().map { it.code }
          val allAppointmentCategories = prisonApiClient.getScheduleReasons(ScheduleReasonEventType.APPOINTMENT.value).map { it.code }

          allPrisonCodes.forEach { prisonCode ->
            allAppointmentCategories.forEach { appointmentCategory ->
              sendAppointmentsDailyStatsEvent(prisonCode, appointmentCategory)
            }
          }
        }

        log.info("Generating daily appointments metrics took ${elapsed}ms")
      },
    )
  }

  private fun sendAppointmentsDailyStatsEvent(
    prisonCode: String,
    appointmentCategory: String,
  ) {
    val yesterday = LocalDate.now().minusDays(1)

    val propertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to prisonCode,
      CATEGORY_CODE_PROPERTY_KEY to appointmentCategory,
    )

    val metricsMap = mutableMapOf(
      APPOINTMENT_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_SERIES_COUNT_METRIC_KEY to 0.0,
      APPOINTMENT_SET_COUNT_METRIC_KEY to 0.0,
      CANCELLED_APPOINTMENT_COUNT_METRIC_KEY to 0.0,
      DELETED_APPOINTMENT_COUNT_METRIC_KEY to 0.0,
    )

    val appointments = appointmentRepository.findByPrisonCodeAndCategoryCodeAndDate(prisonCode, appointmentCategory, yesterday)
    dailyAppointmentMetricsService.generateAppointmentMetrics(metricsMap, appointments)

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value, propertiesMap, metricsMap)
  }
}
