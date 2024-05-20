package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

/* Property Keys */
const val ACKNOWLEDGED_BY_PROPERTY_KEY = "acknowledgedBy"
const val ACKNOWLEDGED_TIME_PROPERTY_KEY = "acknowledgedTime"
const val ACTIVITY_ID_PROPERTY_KEY = "activityId"
const val ACTIVITY_CATEGORY_PROPERTY_KEY = "activityCategory"
const val ACTIVITY_NAME_PROPERTY_KEY = "activityName"
const val ACTIVITY_SUMMARY_PROPERTY_KEY = "activitySummary"
const val SCHEDULED_INSTANCE_ID_PROPERTY_KEY = "scheduledInstanceId"
const val ALLOCATION_START_DATE_PROPERTY_KEY = "allocationStartDate"
const val ALLOCATION_REQUEST_DATE_PROPERTY_KEY = "requestDate"
const val ATTENDANCE_REASON_PROPERTY_KEY = "attendanceReason"
const val ATTENDED_BEFORE_SESSION_ENDED_PROPERTY_KEY = "attendedBeforeSessionEnded"
const val APPLY_TO_PROPERTY_KEY = "applyTo"
const val APPOINTMENT_ID_PROPERTY_KEY = "appointmentId"
const val APPOINTMENT_SERIES_ID_PROPERTY_KEY = "appointmentSeriesId"
const val APPOINTMENT_SET_ID_PROPERTY_KEY = "appointmentSetId"
const val ORIGINAL_ID_PROPERTY_KEY = "originalId"
const val CATEGORY_CHANGED_PROPERTY_KEY = "categoryChanged"
const val CATEGORY_CODE_PROPERTY_KEY = "categoryCode"
const val CATEGORY_DESCRIPTION_PROPERTY_KEY = "categoryDescription"
const val CREATED_BY_PROPERTY_KEY = "createdBy"
const val EARLIEST_START_TIME_PROPERTY_KEY = "earliestStartTime"
const val END_DATE_PROPERTY_KEY = "endDate"
const val END_TIME_CHANGED_PROPERTY_KEY = "endTimeChanged"
const val END_TIME_PROPERTY_KEY = "endTime"
const val EVENT_ORGANISER_CHANGED_PROPERTY_KEY = "eventOrganiserChanged"
const val EVENT_ORGANISER_PROPERTY_KEY = "eventOrganiser"
const val EVENT_REVIEW_IDS_PROPERTY_KEY = "eventReviewIds"
const val EVENT_TIER_CHANGED_PROPERTY_KEY = "eventTierChanged"
const val EVENT_TIER_PROPERTY_KEY = "eventTier"
const val EXTRA_INFORMATION_CHANGED_PROPERTY_KEY = "extraInformationChanged"
const val FREQUENCY_PROPERTY_KEY = "frequency"
const val HAS_CUSTOM_NAME_PROPERTY_KEY = "hasCustomName"
const val HAS_EXTRA_INFORMATION_PROPERTY_KEY = "hasExtraInformation"
const val IS_REPEAT_PROPERTY_KEY = "isRepeat"
const val INTERNAL_LOCATION_ID_PROPERTY_KEY = "internalLocationId"
const val INTERNAL_LOCATION_CHANGED_PROPERTY_KEY = "internalLocationChanged"
const val INTERNAL_LOCATION_DESCRIPTION_PROPERTY_KEY = "internalLocationDescription"
const val LATEST_END_TIME_PROPERTY_KEY = "latestEndTime"
const val NUMBER_OF_APPOINTMENTS_PROPERTY_KEY = "numberOfAppointments"
const val PRISON_CODE_PROPERTY_KEY = "prisonCode"
const val PRISONER_NUMBER_PROPERTY_KEY = "prisonerNumber"
const val START_DATE_CHANGED_PROPERTY_KEY = "startDateChanged"
const val START_DATE_PROPERTY_KEY = "startDate"
const val START_TIME_CHANGED_PROPERTY_KEY = "startTimeChanged"
const val START_TIME_PROPERTY_KEY = "startTime"
const val TIME_SLOT_PROPERTY_KEY = "timeSlot"
const val USER_PROPERTY_KEY = "user"

/* Metric Keys */
const val ACTIVITIES_ACTIVE_COUNT_METRIC_KEY = "activitiesActiveCount"
const val ACTIVITIES_ENDED_COUNT_METRIC_KEY = "activitiesEndedCount"
const val ACTIVITIES_PENDING_COUNT_METRIC_KEY = "activitiesPendingCount"
const val ACTIVITIES_TOTAL_COUNT_METRIC_KEY = "activitiesTotalCount"
const val ALLOCATIONS_ACTIVE_COUNT_METRIC_KEY = "allocationsActiveCount"
const val ALLOCATIONS_AUTO_SUSPENDED_COUNT_METRIC_KEY = "allocationsAutoSuspendedCount"
const val ALLOCATIONS_DELETED_COUNT_METRIC_KEY = "allocationsDeletedCount"
const val ALLOCATIONS_ENDED_COUNT_METRIC_KEY = "allocationsEndedCount"
const val ALLOCATIONS_PENDING_COUNT_METRIC_KEY = "allocationsPendingCount"
const val ALLOCATIONS_SUSPENDED_COUNT_METRIC_KEY = "allocationsSuspendedCount"
const val ALLOCATIONS_TOTAL_COUNT_METRIC_KEY = "allocationsTotalCount"
const val APPLICATIONS_APPROVED_COUNT_METRIC_KEY = "applicationsApprovedCount"
const val APPLICATIONS_PENDING_COUNT_METRIC_KEY = "applicationsPendingCount"
const val APPLICATIONS_DECLINED_COUNT_METRIC_KEY = "applicationsDeclinedCount"
const val APPLICATIONS_TOTAL_COUNT_METRIC_KEY = "applicationsTotalCount"
const val APPOINTMENT_COUNT_METRIC_KEY = "appointmentCount"
const val APPOINTMENT_INSTANCE_COUNT_METRIC_KEY = "appointmentInstanceCount"
const val APPOINTMENT_SERIES_COUNT_METRIC_KEY = "appointmentSeriesCount"
const val APPOINTMENT_SET_COUNT_METRIC_KEY = "appointmentSetCount"
const val ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY = "attendanceAcceptableAbsenceCount"
const val ATTENDANCE_ATTENDED_COUNT_METRIC_KEY = "attendanceAttendedCount"
const val ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY = "attendanceUniqueActivitySessionCount"
const val ATTENDANCE_RECORDED_COUNT_METRIC_KEY = "attendanceRecordedCount"
const val ATTENDANCE_RECORDED_ATTENDED_COUNT_METRIC_KEY = "attendanceRecordedAttendedCount"
const val ATTENDANCE_RECORDED_NOT_ATTENDED_COUNT_METRIC_KEY = "attendanceRecordedNotAttendedCount"
const val ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY = "attendanceUnacceptableAbsenceCount"
const val CANCELLED_APPOINTMENT_COUNT_METRIC_KEY = "cancelledAppointmentCount"
const val CUSTOM_NAME_LENGTH_METRIC_KEY = "customNameLength"
const val DELETED_APPOINTMENT_COUNT_METRIC_KEY = "deletedAppointmentCount"
const val EVENT_TIME_MS_METRIC_KEY = "eventTimeMs"
const val EXTRA_INFORMATION_COUNT_METRIC_KEY = "extraInformationCount"
const val EXTRA_INFORMATION_LENGTH_METRIC_KEY = "extraInformationLength"
const val MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY = "multiWeekActivitiesCount"
const val NUMBER_OF_RESULTS_METRIC_KEY = "numberOfResults"
const val PRISONER_COUNT_METRIC_KEY = "prisonerCount"
const val PRISONERS_ADDED_COUNT_METRIC_KEY = "prisonersAddedCount"
const val PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY = "prisonersAttendanceChangedCount"
const val PRISONERS_ATTENDED_COUNT_METRIC_KEY = "prisonersAttendedCount"
const val PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY = "prisonersNonAttendedCount"
const val PRISONERS_REMOVED_COUNT_METRIC_KEY = "prisonersRemovedCount"
const val RESULTS_COUNT_METRIC_KEY = "resultsCount"
const val WAIT_BEFORE_ALLOCATION_METRIC_KEY = "applicationWaitBeforeAllocationTimeDays"

fun activityMetricsMap() = mapOf(
  NUMBER_OF_RESULTS_METRIC_KEY to 1.0,
)
