package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

enum class TelemetryEvent(val value: String) {

  ACTIVITY_CREATED("SAA-CreateActivity"),
  PRISONER_ADDED_TO_WAITLIST("SAA-PrisonerAddedToWaitlist"),
  PRISONER_DECLINED_FROM_WAITLIST("SAA-PrisonerDeclinedFromWaitlist"),
  PRISONER_APPROVED_ON_WAITLIST("SAA-PrisonerApporvedOnWaitlist"),
}
