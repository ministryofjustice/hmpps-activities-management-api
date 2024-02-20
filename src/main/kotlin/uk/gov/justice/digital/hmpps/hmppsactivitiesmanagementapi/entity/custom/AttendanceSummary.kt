package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.custom

data class AttendanceSummary(
  val attendanceReasonCode: String,
  val count: Long,
)
