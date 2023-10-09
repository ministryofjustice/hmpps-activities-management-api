package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun Allocation.createAllocationTelemetryPropertiesMap(maybeWaitingList: WaitingList?): MutableMap<String, String> {
  val propertiesMap = mutableMapOf(
    USER_PROPERTY_KEY to allocatedBy,
    PRISON_CODE_PROPERTY_KEY to activitySchedule.activity.prisonCode,
    PRISONER_NUMBER_PROPERTY_KEY to prisonerNumber,
    ACTIVITY_ID_PROPERTY_KEY to activitySchedule.activityScheduleId.toString(),
    ALLOCATION_START_DATE_PROPERTY_KEY to startDate.toString(),
  )
  maybeWaitingList?.let {
    propertiesMap[ALLOCATION_REQUEST_DATE_PROPERTY_KEY] = it.applicationDate.toString()
  }
  return propertiesMap
}

fun Allocation.createAllocationTelemetryMetricsMap(maybeWaitingList: WaitingList?): MutableMap<String, Double> {
  val metricsMap = mutableMapOf<String, Double>()
  maybeWaitingList?.let {
    metricsMap[WAIT_BEFORE_ALLOCATION_METRIC_KEY] =
      ChronoUnit.DAYS.between(it.applicationDate, allocatedTime.toLocalDate()).toDouble()
  }
  return metricsMap
}

fun Attendance.toTelemetryPropertiesMap(): MutableMap<String, String> {
  val endTime = LocalDateTime.of(scheduledInstance.sessionDate, scheduledInstance.endTime)
  val attendedBeforeSessionEnded = recordedTime?.isBefore(endTime) ?: false
  return mutableMapOf(
    USER_PROPERTY_KEY to (recordedBy ?: ""),
    PRISON_CODE_PROPERTY_KEY to scheduledInstance.activitySchedule.activity.prisonCode,
    PRISONER_NUMBER_PROPERTY_KEY to prisonerNumber,
    ACTIVITY_ID_PROPERTY_KEY to scheduledInstance.activitySchedule.activityScheduleId.toString(),
    SCHEDULED_INSTANCE_ID_PROPERTY_KEY to scheduledInstance.scheduledInstanceId.toString(),
    ACTIVITY_SUMMARY_PROPERTY_KEY to scheduledInstance.activitySchedule.activity.summary,
    ATTENDANCE_REASON_PROPERTY_KEY to attendanceReason?.code.toString(),
    ATTENDED_BEFORE_SESSION_ENDED_PROPERTY_KEY to attendedBeforeSessionEnded.toString(),
  )
}

fun AppointmentAttendanceRequest.toTelemetryPropertiesMap(
  user: String,
  prisonCode: String,
  appointmentId: Long,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to user,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_ID_PROPERTY_KEY to appointmentId.toString(),
  )

fun AppointmentAttendanceRequest.toTelemetryMetricsMap() =
  mutableMapOf(
    PRISONERS_ATTENDED_COUNT_METRIC_KEY to this.attendedPrisonNumbers.size.toDouble(),
    PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY to this.nonAttendedPrisonNumbers.size.toDouble(),
    PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY to 0.0,
    EVENT_TIME_MS_METRIC_KEY to 0.0,
  )

fun AppointmentCancelRequest.toTelemetryPropertiesMap(
  user: String,
  prisonCode: String,
  appointmentSeriesId: Long,
  appointmentId: Long,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to user,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointmentSeriesId.toString(),
    APPOINTMENT_ID_PROPERTY_KEY to appointmentId.toString(),
    APPLY_TO_PROPERTY_KEY to this.applyTo.toString(),
  )

fun AppointmentUpdateRequest.toTelemetryPropertiesMap(
  user: String,
  prisonCode: String,
  appointmentSeriesId: Long,
  appointmentId: Long,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to user,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_SERIES_ID_PROPERTY_KEY to appointmentSeriesId.toString(),
    APPOINTMENT_ID_PROPERTY_KEY to appointmentId.toString(),
    CATEGORY_CHANGED_PROPERTY_KEY to (this.categoryCode != null).toString(),
    INTERNAL_LOCATION_CHANGED_PROPERTY_KEY to (this.internalLocationId != null).toString(),
    START_DATE_CHANGED_PROPERTY_KEY to (this.startDate != null).toString(),
    START_TIME_CHANGED_PROPERTY_KEY to (this.startTime != null).toString(),
    END_TIME_CHANGED_PROPERTY_KEY to (this.endTime != null).toString(),
    EXTRA_INFORMATION_CHANGED_PROPERTY_KEY to (this.extraInformation != null).toString(),
    APPLY_TO_PROPERTY_KEY to this.applyTo.toString(),
  )

fun AppointmentUpdateRequest.toTelemetryMetricsMap(appointmentCount: Int, appointmentInstanceCount: Int) =
  mutableMapOf(
    APPOINTMENT_COUNT_METRIC_KEY to appointmentCount.toDouble(),
    APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to appointmentInstanceCount.toDouble(),
    PRISONERS_REMOVED_COUNT_METRIC_KEY to (this.removePrisonerNumbers?.size?.toDouble() ?: 0.0),
    PRISONERS_ADDED_COUNT_METRIC_KEY to (this.addPrisonerNumbers?.size?.toDouble() ?: 0.0),
    EVENT_TIME_MS_METRIC_KEY to 0.0,
  )
