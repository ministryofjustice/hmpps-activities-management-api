package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.DailyActivityMetricsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_ACTIVE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_ENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_PENDING_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_TOTAL_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITY_CATEGORY_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITY_TIER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_ACTIVE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_AUTO_SUSPENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_DELETED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_ENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_PENDING_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_SUSPENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ALLOCATIONS_TOTAL_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_APPROVED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_DECLINED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_PENDING_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_TOTAL_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_RECORDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@Component
class ActivityMetricsJob(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val eventTierRepository: EventTierRepository,
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val dailyActivityMetricsService: DailyActivityMetricsService,
  private val telemetryClient: TelemetryClient,
  private val jobRunner: SafeJobRunner,

) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    jobRunner.runJob(
      JobDefinition(JobType.ACTIVITIES_METRICS) {
        log.info("Generating daily activities metrics")

        val elapsed = measureTimeMillis {
          val allPrisonCodes = rolloutPrisonRepository.findAll().map { it.code }
          val allEventTiers = eventTierRepository.findAll()
          val allActivityCategories = activityCategoryRepository.findAll()

          allPrisonCodes.forEach { prisonCode ->
            allEventTiers.forEach { eventTier ->
              allActivityCategories.forEach { activityCategory ->
                sendActivitiesDailyStatsEvent(prisonCode, eventTier, activityCategory)
              }
            }
          }
        }

        log.info("Generating daily activities metrics took ${elapsed}ms")
      },
    )
  }

  private fun sendActivitiesDailyStatsEvent(
    prisonCode: String,
    activityTier: EventTier,
    activityCategory: ActivityCategory,
  ) {
    val yesterday = LocalDate.now().minusDays(1)

    val propertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to prisonCode,
      ACTIVITY_TIER_PROPERTY_KEY to activityTier.code,
      ACTIVITY_CATEGORY_PROPERTY_KEY to activityCategory.code,
    )

    val metricsMap = mutableMapOf(
      ACTIVITIES_ACTIVE_COUNT_METRIC_KEY to 0.0,
      ACTIVITIES_ENDED_COUNT_METRIC_KEY to 0.0,
      ACTIVITIES_PENDING_COUNT_METRIC_KEY to 0.0,
      ACTIVITIES_TOTAL_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_ACTIVE_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_AUTO_SUSPENDED_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_DELETED_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_ENDED_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_PENDING_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_SUSPENDED_COUNT_METRIC_KEY to 0.0,
      ALLOCATIONS_TOTAL_COUNT_METRIC_KEY to 0.0,
      APPLICATIONS_APPROVED_COUNT_METRIC_KEY to 0.0,
      APPLICATIONS_PENDING_COUNT_METRIC_KEY to 0.0,
      APPLICATIONS_DECLINED_COUNT_METRIC_KEY to 0.0,
      APPLICATIONS_TOTAL_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_ATTENDED_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_RECORDED_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY to 0.0,
      MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY to 0.0,
    )

    val activities = activityRepository.findByPrisonCodeAndActivityTierAndActivityCategory(prisonCode, activityTier, activityCategory)
    dailyActivityMetricsService.generateActivityMetrics(yesterday, metricsMap, activities)

    telemetryClient.trackEvent(TelemetryEvent.ACTIVITIES_DAILY_STATS.value, propertiesMap, metricsMap)
  }
}
