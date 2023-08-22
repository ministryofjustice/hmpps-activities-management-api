package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

enum class TelemetryEvent(val value: String) {

  ACTIVITY_CREATED("SAA-CreateActivity"),
  APPOINTMENT_CANCELLED("SAA-Appointments-Appointment-Cancelled"),
  APPOINTMENT_CREATED("SAA-Appointments-Appointment-Created"),
  APPOINTMENT_DELETED("SAA-Appointments-Appointment-Deleted"),
  APPOINTMENT_EDITED("SAA-Appointments-Appointment-Edited"),
  APPOINTMENT_SEARCH("SAA-Appointments-Search"),
  APPOINTMENT_SET_CREATED("SAA-Appointments-Appointment-Set-Created"),
  COC("SAA-ChangeOfCircumstances"),
  EDIT_ACTIVITY("SAA-EditActivity"),
  PRISONER_ADDED_TO_WAITLIST("SAA-PrisonerAddedToWaitlist"),
  PRISONER_ALLOCATED("SAA-PrisonerAllocated"),
  PRISONER_APPROVED_ON_WAITLIST("SAA-PrisonerApporvedOnWaitlist"),
  PRISONER_DEALLOCATED("SAA-PrisonerDeallocated"),
  PRISONER_DECLINED_FROM_WAITLIST("SAA-PrisonerDeclinedFromWaitlist"),
}
