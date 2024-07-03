package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendanceMarkedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

fun AppointmentAttendanceMarkedEvent.toTelemetryPropertiesMap() =
  mutableMapOf(
    USER_PROPERTY_KEY to attendanceRecordedBy,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_ID_PROPERTY_KEY to appointmentId.toString(),
  )

fun AppointmentAttendanceMarkedEvent.toTelemetryMetricsMap() =
  mutableMapOf(
    PRISONERS_ATTENDED_COUNT_METRIC_KEY to attendedPrisonNumbers.size.toDouble(),
    PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY to nonAttendedPrisonNumbers.size.toDouble(),
    PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY to attendanceChangedPrisonNumbers.size.toDouble(),
    EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - ZonedDateTime.of(attendanceRecordedTime, ZoneId.systemDefault()).toInstant().toEpochMilli()).toDouble(),
  )

fun AppointmentSet.toTelemetryPropertiesMap(
  categoryDescription: String,
  internalLocationDescription: String,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to createdBy,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_SET_ID_PROPERTY_KEY to id.toString(),
    CATEGORY_CODE_PROPERTY_KEY to categoryCode,
    CATEGORY_DESCRIPTION_PROPERTY_KEY to categoryDescription,
    HAS_CUSTOM_NAME_PROPERTY_KEY to (customName?.trim()?.takeUnless(String::isBlank) != null).toString(),
    INTERNAL_LOCATION_ID_PROPERTY_KEY to (if (inCell) "" else internalLocationId?.toString() ?: ""),
    INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY to internalLocationDescription,
    START_DATE_PROPERTY_KEY to startDate.toString(),
    EARLIEST_START_TIME_PROPERTY_KEY to appointments.mapNotNull { it.startTime }.takeUnless { it.isEmpty() }?.minOf { it }.toString(),
    LATEST_END_TIME_PROPERTY_KEY to appointments.mapNotNull { it.endTime }.takeUnless { it.isEmpty() }?.maxOf { it }.toString(),
    EVENT_TIER_PROPERTY_KEY to (tier?.description ?: ""),
    EVENT_ORGANISER_PROPERTY_KEY to (organiser?.description ?: ""),
  )

fun AppointmentSet.toTelemetryMetricsMap() =
  mutableMapOf(
    PRISONER_COUNT_METRIC_KEY to appointments.flatMap { app -> app.attendees.map { it.prisonerNumber } }.distinct().size.toDouble(),
    APPOINTMENT_COUNT_METRIC_KEY to appointments.size.toDouble(),
    APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to appointments.size.toDouble(),
    CUSTOM_NAME_LENGTH_METRIC_KEY to (customName?.trim()?.length ?: 0).toDouble(),
    EXTRA_INFORMATION_COUNT_METRIC_KEY to appointments.mapNotNull { it.extraInformation?.trim()?.takeUnless(String::isBlank) }.size.toDouble(),
    EVENT_TIME_MS_METRIC_KEY to 0.0,
  )

fun AppointmentUpdateRequest.toTelemetryPropertiesMap(
  user: String,
  prisonCode: String,
  updatedAppointmentSeries: AppointmentSeries,
  updatedAppointment: Appointment?,
) =
  mutableMapOf(
    USER_PROPERTY_KEY to user,
    PRISON_CODE_PROPERTY_KEY to prisonCode,
    APPOINTMENT_SERIES_ID_PROPERTY_KEY to updatedAppointmentSeries.id.toString(),
    APPOINTMENT_ID_PROPERTY_KEY to updatedAppointment?.id.toString(),
    EVENT_TIER_PROPERTY_KEY to (updatedAppointment?.tier?.description ?: ""),
    EVENT_ORGANISER_PROPERTY_KEY to (updatedAppointment?.organiser?.description ?: ""),
    CATEGORY_CHANGED_PROPERTY_KEY to (this.categoryCode != null).toString(),
    EVENT_TIER_CHANGED_PROPERTY_KEY to (this.tierCode != null).toString(),
    EVENT_ORGANISER_CHANGED_PROPERTY_KEY to (this.organiserCode != null).toString(),
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

fun AppointmentUncancelRequest.toTelemetryPropertiesMap(
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
