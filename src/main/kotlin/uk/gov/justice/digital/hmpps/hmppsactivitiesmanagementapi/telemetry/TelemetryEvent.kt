package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

enum class TelemetryEvent(val value: String) {

  ACTIVITIES_DAILY_STATS("SAA-Activities-Daily-Stats"),
  ACTIVITY_CREATED("SAA-Activity-Created"),
  ACTIVITY_EDITED("SAA-Activity-Edited"),
  RECORD_ATTENDANCE("SAA-Record-Attendance"),
  CREATE_ALLOCATION("SAA-Create-Allocation"),
  APPOINTMENT_CANCELLED("SAA-Appointment-Cancelled"),
  APPOINTMENT_SERIES_CREATED("SAA-Appointment-Series-Created"),
  APPOINTMENT_DELETED("SAA-Appointment-Deleted"),
  APPOINTMENT_EDITED("SAA-Appointment-Edited"),
  APPOINTMENT_SEARCH("SAA-Appointment-Search"),
  APPOINTMENT_SET_CREATED("SAA-Appointment-Set-Created"),
  APPOINTMENT_UNCANCELLED("SAA-Appointment-Uncancelled"),
  APPOINTMENT_ATTENDANCE_MARKED_METRICS("SAA-Appointment-Attendance-Marked"),
  APPOINTMENTS_AGGREGATE_METRICS("SAA-Appointments-Aggregate-Metrics"),
  COC("SAA-Activity-Circumstances-Changed"),
  PRISONER_ADDED_TO_WAITLIST("SAA-Prisoner-Added-To-Waitlist"),
  PRISONER_APPROVED_ON_WAITLIST("SAA-Prisoner-Apporved-On-Waitlist"),
  PRISONER_DEALLOCATED("SAA-PrisonerDeallocated"),
  PRISONER_DECLINED_FROM_WAITLIST("SAA-Prisoner-Declined-From-Waitlist"),
}
