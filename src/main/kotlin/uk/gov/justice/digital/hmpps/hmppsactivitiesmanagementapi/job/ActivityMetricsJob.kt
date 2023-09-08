package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_PENDING_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_REJECTED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_TOTAL_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_RECORDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import kotlin.system.measureTimeMillis

@Component
class ActivityMetricsJob(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val activityTierRepository: ActivityTierRepository,
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val dailyActivityMetricsService: DailyActivityMetricsService,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  @Transactional
  fun execute() {
    log.info("Generating daily activities metrics")

    val elapsed = measureTimeMillis {
      val allPrisonCodes = rolloutPrisonRepository.findAll().map { it.code }
      val allActivityTiers = activityTierRepository.findAll()
      val allActivityCategories = activityCategoryRepository.findAll()

      allPrisonCodes.forEach { prisonCode ->
        allActivityTiers.forEach { activityTier ->
          allActivityCategories.forEach { activityCategory ->
            sendActivitiesDailyStatsEvent(prisonCode, activityTier, activityCategory)
          }
        }
      }
    }

    log.info("Generating daily activities metrics took ${elapsed}ms")
  }

  private fun sendActivitiesDailyStatsEvent(
    prisonCode: String,
    activityTier: ActivityTier,
    activityCategory: ActivityCategory,
  ) {
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
      APPLICATIONS_REJECTED_COUNT_METRIC_KEY to 0.0,
      APPLICATIONS_TOTAL_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_ATTENDED_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_RECORDED_COUNT_METRIC_KEY to 0.0,
      ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY to 0.0,
      MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY to 0.0,
    )

    val activities = activityRepository.findByPrisonCodeAndActivityTierAndActivityCategory(prisonCode, activityTier, activityCategory)
    dailyActivityMetricsService.generateActivityMetrics(metricsMap, activities)

    telemetryClient.trackEvent(TelemetryEvent.ACTIVITIES_DAILY_STATS.value, propertiesMap, metricsMap)
  }
}
