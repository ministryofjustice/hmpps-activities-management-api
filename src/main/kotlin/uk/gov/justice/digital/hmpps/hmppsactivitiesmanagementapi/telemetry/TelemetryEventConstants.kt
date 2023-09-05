package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

/* Property Keys */
const val ACKNOWLEDGED_BY_PROPERTY_KEY = "acknowledgedBy"
const val ACKNOWLEDGED_TIME_PROPERTY_KEY = "acknowledgedTime"
const val ACTIVITY_CATEGORY_PROPERTY_KEY = "activityName"
const val ACTIVITY_NAME_PROPERTY_KEY = "activityName"
const val ACTIVITY_TIER_PROPERTY_KEY = "activityTier"
const val APPLY_TO_PROPERTY_KEY = "applyTo"
const val APPOINTMENT_ID_PROPERTY_KEY = "appointmentId"
const val APPOINTMENT_SERIES_ID_PROPERTY_KEY = "appointmentSeriesId"
const val APPOINTMENT_SET_ID_PROPERTY_KEY = "appointmentSetId"
const val CATEGORY_CHANGED_PROPERTY_KEY = "categoryChanged"
const val CATEGORY_CODE_PROPERTY_KEY = "categoryCode"
const val EARLIEST_START_TIME_PROPERTY_KEY = "earliestStartTime"
const val END_DATE_PROPERTY_KEY = "endDate"
const val END_TIME_CHANGED_PROPERTY_KEY = "endTimeChanged"
const val END_TIME_PROPERTY_KEY = "endTime"
const val EVENT_REVIEW_IDS_PROPERTY_KEY = "eventReviewIds"
const val EXTRA_INFORMATION_CHANGED_PROPERTY_KEY = "extraInformationChanged"
const val HAS_DESCRIPTION_PROPERTY_KEY = "hasDescription"
const val HAS_EXTRA_INFORMATION_PROPERTY_KEY = "hasExtraInformation"
const val IS_REPEAT_PROPERTY_KEY = "isRepeat"
const val INTERNAL_LOCATION_ID_PROPERTY_KEY = "internalLocationId"
const val INTERNAL_LOCATION_CHANGED_PROPERTY_KEY = "internalLocationChanged"
const val LATEST_END_TIME_PROPERTY_KEY = "latestEndTime"
const val PRISON_CODE_PROPERTY_KEY = "prisonCode"
const val PRISONER_NUMBER_PROPERTY_KEY = "prisonerNumber"
const val REPEAT_COUNT_PROPERTY_KEY = "repeatCount"
const val REPEAT_PERIOD_PROPERTY_KEY = "repeatPeriod"
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
const val APPLICATIONS_REJECTED_COUNT_METRIC_KEY = "applicationsRejectedCount"
const val APPLICATIONS_TOTAL_COUNT_METRIC_KEY = "applicationsTotalCount"
const val APPOINTMENT_COUNT_METRIC_KEY = "appointmentCount"
const val APPOINTMENT_INSTANCE_COUNT_METRIC_KEY = "appointmentInstanceCount"
const val ATTENDANCE_ACCEPTABLE_ABSENCE_COUNT_METRIC_KEY = "attendanceAcceptableAbsenceCount"
const val ATTENDANCE_ATTENDED_COUNT_METRIC_KEY = "attendanceAttendedCount"
const val ATTENDANCE_UNIQUE_ACTIVITY_SESSION_COUNT_METRIC_KEY = "attendanceUniqueActivitySessionCount"
const val ATTENDANCE_RECORDED_COUNT_METRIC_KEY = "attendanceRecordedCount"
const val ATTENDANCE_UNACCEPTABLE_ABSENCE_COUNT_METRIC_KEY = "attendanceUnacceptableAbsenceCount"
const val DESCRIPTION_LENGTH_METRIC_KEY = "descriptionLength"
const val EVENT_TIME_MS_METRIC_KEY = "eventTimeMs"
const val EXTRA_INFORMATION_COUNT_METRIC_KEY = "extraInformationCount"
const val EXTRA_INFORMATION_LENGTH_METRIC_KEY = "extraInformationLength"
const val MULTI_WEEK_ACTIVITIES_COUNT_METRIC_KEY = "multiWeekActivitiesCount"
const val NUMBER_OF_RESULTS_METRIC_KEY = "numberOfResults"
const val PRISONER_COUNT_METRIC_KEY = "prisonerCount"
const val PRISONERS_ADDED_COUNT_METRIC_KEY = "prisonersAddedCount"
const val PRISONERS_REMOVED_COUNT_METRIC_KEY = "prisonersRemovedCount"
const val RESULTS_COUNT_METRIC_KEY = "resultsCount"

fun activityMetricsMap() = mapOf(
  NUMBER_OF_RESULTS_METRIC_KEY to 1.0,
)
