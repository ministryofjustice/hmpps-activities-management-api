package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

enum class AuditEventType {
  ACTIVITY_CREATED,
  ACTIVITY_UPDATED,
  APPOINTMENT_CANCELLED,
  APPOINTMENT_CANCELLED_ON_TRANSFER,
  APPOINTMENT_SERIES_CREATED,
  APPOINTMENT_DELETED,
  APPOINTMENT_EDITED,
  APPOINTMENT_UNCANCELLED,
  BONUS_PAYMENT_MADE_FOR_ACTIVITY_ATTENDANCE, // Needs renaming or DB column length needs increasing
  APPOINTMENT_SET_CREATED,
  INCENTIVE_LEVEL_WARNING_GIVEN_FOR_ACTIVITY_ATTENDANCE, // Needs renaming or DB column length needs increasing
  PRISONER_ACCEPTED_FROM_WAITING_LIST,
  PRISONER_ADDED_TO_WAITING_LIST,
  PRISONER_ALLOCATED,
  PRISONER_DEALLOCATED,
  PRISONER_DECLINED_FROM_WAITING_LIST,
  PRISONER_REMOVED_FROM_WAITING_LIST,
  PRISONER_SUSPENDED_FROM_ACTIVITY,
  PRISONER_UNSUSPENDED_FROM_ACTIVITY,
  PRISONER_MERGE,
}
