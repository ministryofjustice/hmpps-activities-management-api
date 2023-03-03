package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

enum class EventType(val defaultPriority: Int) {
  COURT_HEARING(1),
  VISIT(2),
  ADJUDICATION_HEARING(3),
  APPOINTMENT(4),
  ACTIVITY(5),
}
