package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
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
import java.time.LocalDate

@Component
class ActivityMetricsJob(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val activityTierRepository: ActivityTierRepository,
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
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
    generateActivityMetrics(metricsMap, activities)

    telemetryClient.trackEvent(TelemetryEvent.ACTIVITIES_DAILY_STATS.value, propertiesMap, metricsMap)
  }

  private fun generateActivityMetrics(metricsMap: MutableMap<String, Double>, activities: List<Activity>) {
    activities.forEach {
      incrementMetric(metricsMap, ACTIVITIES_TOTAL_COUNT_METRIC_KEY)
      if (it.isActive(LocalDate.now())) {
        incrementMetric(metricsMap, ACTIVITIES_ACTIVE_COUNT_METRIC_KEY)
      } else {
        if (it.endDate?.isBefore(LocalDate.now()) == true) {
          incrementMetric(metricsMap, ACTIVITIES_ENDED_COUNT_METRIC_KEY)
        } else {
          incrementMetric(metricsMap, ACTIVITIES_PENDING_COUNT_METRIC_KEY)
        }
      }

      // TODO How to know if an activity is multi-week?

      val allocations = it.schedules().flatMap { schedules -> schedules.allocations() }

      generateAllocationMetrics(metricsMap, allocations)

      // TODO Where are the applications?
    }
  }

  private fun generateAllocationMetrics(metricsMap: MutableMap<String, Double>, allocations: List<Allocation>) {
    incrementMetric(metricsMap, ALLOCATIONS_TOTAL_COUNT_METRIC_KEY)
    allocations.forEach {
      when (it.prisonerStatus) {
        PrisonerStatus.ENDED -> incrementMetric(metricsMap, ALLOCATIONS_ENDED_COUNT_METRIC_KEY)
        PrisonerStatus.ACTIVE -> incrementMetric(metricsMap, ALLOCATIONS_ACTIVE_COUNT_METRIC_KEY)
        PrisonerStatus.SUSPENDED -> incrementMetric(metricsMap, ALLOCATIONS_SUSPENDED_COUNT_METRIC_KEY)
        PrisonerStatus.AUTO_SUSPENDED -> incrementMetric(metricsMap, ALLOCATIONS_AUTO_SUSPENDED_COUNT_METRIC_KEY)
        PrisonerStatus.PENDING -> incrementMetric(metricsMap, ALLOCATIONS_PENDING_COUNT_METRIC_KEY)
      }

      // TODO How to work out if one has been deleted?
    }
  }

  private fun incrementMetric(metricsMap: MutableMap<String, Double>, metricKey: String, increment: Int = 1) {
    metricsMap[metricKey] = ((metricsMap[metricKey] ?: 0.0) + increment)
  }
}
