package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

enum class EventType(val defaultPriority: Int) {
  COURT_HEARING(1),
  EXTERNAL_TRANSFER(2),
  VISIT(3),
  ADJUDICATION_HEARING(4),
  APPOINTMENT(5),
  ACTIVITY(6),
}
