package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_DECLINED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_PENDING_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPLICATIONS_TOTAL_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_RECORDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class DailyActivityMetricsServiceTest {

  private val attendanceReasonAttended: AttendanceReason = mock()
  private val attendanceReasonRefused: AttendanceReason = mock()
  private val attendanceReasonOther: AttendanceReason = mock()
  private val attendanceReasonClash: AttendanceReason = mock()

  private val allocationEnded: Allocation = mock()
  private val allocationActive: Allocation = mock()
  private val allocationSuspended: Allocation = mock()
  private val allocationAutoSuspended: Allocation = mock()
  private val allocationPending: Allocation = mock()

  private val activityActive: Activity = activityEntity()
  private val activityPending: Activity = activityEntity(startDate = LocalDate.now().plusDays(2), endDate = null)
  private val activityEnded: Activity =
    activityEntity(startDate = LocalDate.now().minusDays(4), endDate = LocalDate.now().minusDays(2))
  private val multiWeekSchedule: ActivitySchedule = mock()
  private val multiWeekActivity: Activity =
    activityEntity(startDate = LocalDate.now().minusDays(4), noSchedules = true)

  private val scheduledInstance: ScheduledInstance = mock()
  private val waitingListRepository: WaitingListRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()

  private val dailyActivityMetricsService = DailyActivityMetricsService(waitingListRepository, attendanceRepository)

  @Test
  fun `should generate activity metrics`() {
    val metricsMap = createMetricsMap()

    whenever(multiWeekSchedule.startDate).thenReturn(LocalDate.now().minusDays(2))
    whenever(multiWeekSchedule.scheduleWeeks).thenReturn(2)
    whenever(multiWeekSchedule.activity).thenReturn(multiWeekActivity)
    multiWeekActivity.addSchedule(multiWeekSchedule)

    val activities = listOf(
      activityActive,
      activityPending,
      activityPending,
      activityEnded,
      activityEnded,
      activityEnded,
      multiWeekActivity,
      multiWeekActivity,
    )

    val attendances = activities
      .flatMap { activity ->
        activity.schedules()
          .flatMap { schedule -> schedule.instances() }
          .flatMap { instance -> instance.attendances }
      }

    val attendanceLookup = attendances.associateBy { it.attendanceId }

    whenever(attendanceRepository.findById(any())).thenAnswer { Optional.ofNullable(attendanceLookup[it.arguments[0]]) }

    val allAttendances = activities
      .flatMap { activity ->
        activity.schedules()
          .flatMap { schedule -> schedule.instances() }
          .flatMap { instance -> instance.attendances }
          .map { attendance ->
            val allAttendance = mock<AllAttendance>()
            whenever(allAttendance.attendanceId).thenReturn(attendance.attendanceId)
            whenever(allAttendance.activityId).thenReturn(activity.activityId)
            allAttendance
          }
      }

    dailyActivityMetricsService.generateActivityMetrics(LocalDate.now(), metricsMap, activities, allAttendances)

    assertThat(metricsMap[ACTIVITIES_TOTAL_COUNT_METRIC_KEY]).isEqualTo(8.0)
    assertThat(metricsMap[ACTIVITIES_ACTIVE_COUNT_METRIC_KEY]).isEqualTo(3.0)
    assertThat(metricsMap[ACTIVITIES_ENDED_COUNT_METRIC_KEY]).isEqualTo(3.0)
    assertThat(metricsMap[ACTIVITIES_PENDING_COUNT_METRIC_KEY]).isEqualTo(2.0)
    assertThat(metricsMap[MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY]).isEqualTo(2.0)
    assertThat(metricsMap[ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY]).isEqualTo(8.0)
  }

  @Test
  fun `should generate allocation metrics`() {
    val metricsMap = createMetricsMap()

    whenever(allocationEnded.prisonerStatus).thenReturn(PrisonerStatus.ENDED)
    whenever(allocationEnded.deallocatedTime).thenReturn(LocalDateTime.now().minusDays(1))
    whenever(allocationActive.prisonerStatus).thenReturn(PrisonerStatus.ACTIVE)
    whenever(allocationSuspended.prisonerStatus).thenReturn(PrisonerStatus.SUSPENDED)
    whenever(allocationAutoSuspended.prisonerStatus).thenReturn(PrisonerStatus.AUTO_SUSPENDED)
    whenever(allocationPending.prisonerStatus).thenReturn(PrisonerStatus.PENDING)

    val allocations = listOf(
      allocationEnded,
      allocationActive,
      allocationActive,
      allocationSuspended,
      allocationSuspended,
      allocationSuspended,
      allocationPending,
      allocationPending,
      allocationAutoSuspended,
    )

    dailyActivityMetricsService.generateAllocationMetrics(LocalDate.now(), metricsMap, allocations)

    assertThat(metricsMap[ALLOCATIONS_ENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
    assertThat(metricsMap[ALLOCATIONS_ACTIVE_COUNT_METRIC_KEY]).isEqualTo(2.0)
    assertThat(metricsMap[ALLOCATIONS_SUSPENDED_COUNT_METRIC_KEY]).isEqualTo(3.0)
    assertThat(metricsMap[ALLOCATIONS_PENDING_COUNT_METRIC_KEY]).isEqualTo(2.0)
    assertThat(metricsMap[ALLOCATIONS_AUTO_SUSPENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
    assertThat(metricsMap[ALLOCATIONS_DELETED_COUNT_METRIC_KEY]).isEqualTo(1.0)
  }

  @Test
  fun `should generate attendance metrics`() {
    whenever(attendanceReasonAttended.attended).thenReturn(true)
    whenever(attendanceReasonRefused.code).thenReturn(AttendanceReasonEnum.REFUSED)
    whenever(attendanceReasonOther.code).thenReturn(AttendanceReasonEnum.OTHER)
    whenever(attendanceReasonClash.code).thenReturn(AttendanceReasonEnum.CLASH)

    val metricsMap = createMetricsMap()

    val recorded1 = Attendance(
      prisonerNumber = "111",
      scheduledInstance = scheduledInstance,
      recordedTime = LocalDateTime.now(),
    )
    val recorded2 = Attendance(
      prisonerNumber = "112",
      scheduledInstance = scheduledInstance,
      recordedTime = LocalDateTime.now(),
    )
    val attended1 = Attendance(
      prisonerNumber = "113",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonAttended,
    )
    val attended2 = Attendance(
      prisonerNumber = "114",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonAttended,
    )
    val attended3 = Attendance(
      prisonerNumber = "115",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonAttended,
    )
    val refused1 = Attendance(
      prisonerNumber = "116",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonRefused,
    )
    val other1 = Attendance(
      prisonerNumber = "117",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonOther,
    )
    val other2 = Attendance(
      prisonerNumber = "118",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonOther,
    )
    val clash1 = Attendance(
      prisonerNumber = "119",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonClash,
    )
    val clash2 = Attendance(
      prisonerNumber = "120",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonClash,
    )
    val clash3 = Attendance(
      prisonerNumber = "121",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonClash,
    )
    val clash4 = Attendance(
      prisonerNumber = "122",
      scheduledInstance = scheduledInstance,
      attendanceReason = attendanceReasonClash,
    )

    val attendances = listOf(
      recorded1,
      recorded2,
      attended1,
      attended2,
      attended3,
      refused1,
      other1,
      other2,
      clash1,
      clash2,
      clash3,
      clash4,
    )

    dailyActivityMetricsService.generateAttendanceMetrics(metricsMap, attendances)

    assertThat(metricsMap[ATTENDANCE_RECORDED_COUNT_METRIC_KEY]).isEqualTo(2.0)
    assertThat(metricsMap[ATTENDANCE_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(3.0)
    assertThat(metricsMap[ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY]).isEqualTo(3.0)
    assertThat(metricsMap[ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY]).isEqualTo(6.0)
  }

  @Test
  fun `should generate waiting list metrics`() {
    val metricsMap = createMetricsMap()

    val approved1 = waitingList(initialStatus = WaitingListStatus.APPROVED)
    val pending1 = waitingList(initialStatus = WaitingListStatus.PENDING)
    val pending2 = waitingList(initialStatus = WaitingListStatus.PENDING)
    val declined1 = waitingList(initialStatus = WaitingListStatus.DECLINED)
    val declined2 = waitingList(initialStatus = WaitingListStatus.DECLINED)
    val declined3 = waitingList(initialStatus = WaitingListStatus.DECLINED)

    val waitingLists = listOf(approved1, pending1, pending2, declined1, declined2, declined3)

    dailyActivityMetricsService.generateWaitingListMetrics(metricsMap, waitingLists)

    assertThat(metricsMap[APPLICATIONS_TOTAL_COUNT_METRIC_KEY]).isEqualTo(6.0)
    assertThat(metricsMap[APPLICATIONS_APPROVED_COUNT_METRIC_KEY]).isEqualTo(1.0)
    assertThat(metricsMap[APPLICATIONS_PENDING_COUNT_METRIC_KEY]).isEqualTo(2.0)
    assertThat(metricsMap[APPLICATIONS_DECLINED_COUNT_METRIC_KEY]).isEqualTo(3.0)
  }

  private fun createMetricsMap() = mutableMapOf(
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
}
