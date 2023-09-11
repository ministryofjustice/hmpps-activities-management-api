package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Attendance summary details for a scheduled instance")
data class AttendanceSummaryDetails(
  @Schema(description = "The number of prisoners allocated to this scheduled instance", example = "5")
  val allocations: Long,

  @Schema(description = "The number of attendees for this scheduled instance", example = "5")
  val attendees: Long? = null,

  @Schema(description = "The number of attendance records not recorded", example = "2")
  val notRecorded: Long? = null,

  @Schema(description = "The number of attendance recorded marked as attended", example = "2")
  val attended: Long? = null,

  @Schema(description = "The number of attendance recorded marked as absence", example = "1")
  val absences: Long? = null,

  @Schema(description = "The number of attendance recorded marked as paid", example = "2")
  val paid: Long? = null,
)
