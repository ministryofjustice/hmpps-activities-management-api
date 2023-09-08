package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

enum class TelemetryEvent(val value: String) {

  ACTIVITIES_DAILY_STATS("SAA-Activities-Daily-Stats"),
  ACTIVITY_CREATED("SAA-Activity-Created"),
  APPOINTMENT_CANCELLED("SAA-Appointment-Cancelled"),
  APPOINTMENT_SERIES_CREATED("SAA-Appointment-Series-Created"),
  APPOINTMENT_DELETED("SAA-Appointment-Deleted"),
  APPOINTMENT_EDITED("SAA-Appointment-Edited"),
  APPOINTMENT_SEARCH("SAA-Appointments-Search"),
  APPOINTMENT_SET_CREATED("SAA-Appointment-Set-Created"),
  COC("SAA-Activity-Circumstances-Changed"),
  EDIT_ACTIVITY("SAA-Activity-Edited"),
  PRISONER_ADDED_TO_WAITLIST("SAA-Prisoner-Added-To-Waitlist"),
  PRISONER_ALLOCATED("SAA-Prisoner-Allocated"),
  PRISONER_APPROVED_ON_WAITLIST("SAA-Prisoner-Apporved-On-Waitlist"),
  PRISONER_DEALLOCATED("SAA-PrisonerDeallocated"),
  PRISONER_DECLINED_FROM_WAITLIST("SAA-Prisoner-Declined-From-Waitlist"),
}
