package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_ACTIVE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_ENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_PENDING_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITIES_TOTAL_COUNT_METRIC_KEY
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
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class DailyActivityMetricsService(
  private val waitingListRepository: WaitingListRepository,
) {

  fun generateActivityMetrics(metricsMap: MutableMap<String, Double>, activities: List<Activity>) {
    activities.forEach {
      incrementMetric(metricsMap, ACTIVITIES_TOTAL_COUNT_METRIC_KEY)

      if (it.isActive(LocalDate.now())) {
        incrementMetric(metricsMap, ACTIVITIES_ACTIVE_COUNT_METRIC_KEY)
      } else {
        if (it.endDate?.isBefore(LocalDate.now()) == true) {
          incrementMetric(metricsMap, ACTIVITIES_ENDED_COUNT_METRIC_KEY)
        }

        if (it.startDate.isAfter(LocalDate.now())) {
          incrementMetric(metricsMap, ACTIVITIES_PENDING_COUNT_METRIC_KEY)
        }
      }

      if (it.schedules().isNotEmpty() && it.schedules().first().scheduleWeeks > 1) {
        incrementMetric(metricsMap, MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY)
      }

      val attendances = it.schedules().flatMap { schedule -> schedule.instances() }.flatMap { instance -> instance.attendances }
      generateAttendanceMetrics(metricsMap, attendances)

      val allocations = it.schedules().flatMap { schedule -> schedule.allocations() }
      generateAllocationMetrics(metricsMap, allocations)

      val waitingLists = it.schedules().flatMap { schedule -> waitingListRepository.findByActivitySchedule(schedule) }
      generateWaitingListMetrics(metricsMap, waitingLists)

      if (attendances.mapNotNull { attendance -> attendance.recordedBy }.isNotEmpty()) {
        incrementMetric(metricsMap, ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY)
      }
    }
  }

  fun generateAllocationMetrics(metricsMap: MutableMap<String, Double>, allocations: List<Allocation>) {
    allocations.forEach {
      incrementMetric(metricsMap, ALLOCATIONS_TOTAL_COUNT_METRIC_KEY)

      when (it.prisonerStatus) {
        PrisonerStatus.ENDED -> incrementMetric(metricsMap, ALLOCATIONS_ENDED_COUNT_METRIC_KEY)
        PrisonerStatus.ACTIVE -> incrementMetric(metricsMap, ALLOCATIONS_ACTIVE_COUNT_METRIC_KEY)
        PrisonerStatus.SUSPENDED -> incrementMetric(metricsMap, ALLOCATIONS_SUSPENDED_COUNT_METRIC_KEY)
        PrisonerStatus.AUTO_SUSPENDED -> incrementMetric(metricsMap, ALLOCATIONS_AUTO_SUSPENDED_COUNT_METRIC_KEY)
        PrisonerStatus.PENDING -> incrementMetric(metricsMap, ALLOCATIONS_PENDING_COUNT_METRIC_KEY)
      }

      if (it.deallocatedTime?.isBefore(LocalDateTime.now()) == true) {
        incrementMetric(metricsMap, ALLOCATIONS_DELETED_COUNT_METRIC_KEY)
      }
    }
  }

  fun generateWaitingListMetrics(metricsMap: MutableMap<String, Double>, waitingLists: List<WaitingList>) {
    waitingLists.forEach {
      incrementMetric(metricsMap, APPLICATIONS_TOTAL_COUNT_METRIC_KEY)

      if (it.status == WaitingListStatus.APPROVED) {
        incrementMetric(metricsMap, APPLICATIONS_APPROVED_COUNT_METRIC_KEY)
      }

      if (it.status == WaitingListStatus.DECLINED) {
        incrementMetric(metricsMap, APPLICATIONS_REJECTED_COUNT_METRIC_KEY)
      }

      if (it.status == WaitingListStatus.PENDING) {
        incrementMetric(metricsMap, APPLICATIONS_PENDING_COUNT_METRIC_KEY)
      }
    }
  }

  fun generateAttendanceMetrics(metricsMap: MutableMap<String, Double>, attendances: List<Attendance>) {
    attendances.forEach {
      if (it.recordedTime != null) {
        incrementMetric(metricsMap, ATTENDANCE_RECORDED_COUNT_METRIC_KEY)
      }

      if (it.attendanceReason?.attended == true) {
        incrementMetric(metricsMap, ATTENDANCE_ATTENDED_COUNT_METRIC_KEY)
      } else {
        if (it.attendanceReason?.code == AttendanceReasonEnum.REFUSED || it.attendanceReason?.code == AttendanceReasonEnum.OTHER) {
          incrementMetric(metricsMap, ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY)
        } else {
          incrementMetric(metricsMap, ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY)
        }
      }
    }
  }

  private fun incrementMetric(metricsMap: MutableMap<String, Double>, metricKey: String, increment: Int = 1) {
    metricsMap[metricKey] = ((metricsMap[metricKey] ?: 0.0) + increment)
  }
}
