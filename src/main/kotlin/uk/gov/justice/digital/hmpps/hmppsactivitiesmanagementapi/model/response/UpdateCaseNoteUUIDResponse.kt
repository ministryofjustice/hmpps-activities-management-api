package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

data class UpdateCaseNoteUUIDResponse(
  val attendance: String,
  val attendanceHistory: String,
  val allocation: String,
  val plannedDeallocation: String,
  val plannedSuspension: String,
)
