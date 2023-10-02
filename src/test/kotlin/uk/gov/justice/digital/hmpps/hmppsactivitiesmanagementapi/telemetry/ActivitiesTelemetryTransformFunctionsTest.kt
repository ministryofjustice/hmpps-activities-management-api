package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import java.time.LocalDate

class ActivitiesTelemetryTransformFunctionsTest {
  @Test
  fun `Allocation properties without waiting list application`() {
    val allocation = activitySchedule(
      activityEntity(
        startDate = LocalDate.of(2023, 1, 10),
      ),
    ).allocations().first()
    val properties = allocation.createAllocationTelemetryPropertiesMap(null)
    properties isEqualTo mutableMapOf(
      USER_PROPERTY_KEY to "Mr Blogs",
      PRISON_CODE_PROPERTY_KEY to "MDI",
      PRISONER_NUMBER_PROPERTY_KEY to "A1234AA",
      ACTIVITY_ID_PROPERTY_KEY to "1",
      ALLOCATION_START_DATE_PROPERTY_KEY to "2023-01-10",
    )
    properties.contains(ALLOCATION_REQUEST_DATE_PROPERTY_KEY) isEqualTo false
  }

  @Test
  fun `Allocation properties with waiting list application`() {
    val allocation = activitySchedule(
      activityEntity(
        startDate = LocalDate.of(2023, 1, 10),
      ),
    ).allocations().first()
    val waitingList = waitingList(applicationDate = LocalDate.of(2023, 2, 1))
    allocation.createAllocationTelemetryPropertiesMap(waitingList) isEqualTo mutableMapOf(
      USER_PROPERTY_KEY to "Mr Blogs",
      PRISON_CODE_PROPERTY_KEY to "MDI",
      PRISONER_NUMBER_PROPERTY_KEY to "A1234AA",
      ACTIVITY_ID_PROPERTY_KEY to "1",
      ALLOCATION_START_DATE_PROPERTY_KEY to "2023-01-10",
      ALLOCATION_REQUEST_DATE_PROPERTY_KEY to "2023-02-01",
    )
  }

  @Test
  fun `Allocation metrics without waiting list application`() {
    val allocation = activitySchedule(
      activityEntity(
        startDate = LocalDate.of(2023, 1, 10),
      ),
    ).allocations().first()
    val metrics = allocation.createAllocationTelemetryMetricsMap(null)
    metrics isEqualTo emptyMap()
  }

  @Test
  fun `Allocation metrics with waiting list application`() {
    val lastWeek = LocalDate.now().minusDays(7)
    val allocation = activitySchedule(
      activityEntity(
        startDate = LocalDate.of(2023, 1, 10),
      ),
    ).allocations().first()
    val waitingList = waitingList(applicationDate = lastWeek)
    allocation.createAllocationTelemetryMetricsMap(waitingList) isEqualTo mutableMapOf(
      WAIT_BEFORE_ALLOCATION_METRIC_KEY to 7.0,
    )
  }

  @Test
  fun `Attendance properties attended before session end`() {
    val tomorrow = LocalDate.now().plusDays(1)
    val activity = activityEntity(startDate = tomorrow)
    val attendance = activity.schedules().first().instances().first().attendances.first().apply {
      attendanceReason = attendanceReasons()["ATTENDED"]
    }
    attendance.toTelemetryPropertiesMap() isEqualTo mutableMapOf(
      USER_PROPERTY_KEY to "Joe Bloggs",
      PRISON_CODE_PROPERTY_KEY to "MDI",
      PRISONER_NUMBER_PROPERTY_KEY to "A1234AA",
      ACTIVITY_ID_PROPERTY_KEY to "1",
      SCHEDULED_INSTANCE_ID_PROPERTY_KEY to "0",
      ACTIVITY_SUMMARY_PROPERTY_KEY to "Maths",
      ATTENDANCE_REASON_PROPERTY_KEY to AttendanceReasonEnum.ATTENDED.toString(),
      ATTENDED_BEFORE_SESSION_ENDED_PROPERTY_KEY to "true",
    )
  }

  @Test
  fun `Attendance properties attended after session end`() {
    val yesterday = LocalDate.now().minusDays(1)
    val activity = activityEntity(startDate = yesterday)
    val attendance = activity.schedules().first().instances().first().attendances.first().apply {
      attendanceReason = attendanceReasons()["ATTENDED"]
    }
    attendance.toTelemetryPropertiesMap() isEqualTo mutableMapOf(
      USER_PROPERTY_KEY to "Joe Bloggs",
      PRISON_CODE_PROPERTY_KEY to "MDI",
      PRISONER_NUMBER_PROPERTY_KEY to "A1234AA",
      ACTIVITY_ID_PROPERTY_KEY to "1",
      SCHEDULED_INSTANCE_ID_PROPERTY_KEY to "0",
      ACTIVITY_SUMMARY_PROPERTY_KEY to "Maths",
      ATTENDANCE_REASON_PROPERTY_KEY to AttendanceReasonEnum.ATTENDED.toString(),
      ATTENDED_BEFORE_SESSION_ENDED_PROPERTY_KEY to "false",
    )
  }
}
