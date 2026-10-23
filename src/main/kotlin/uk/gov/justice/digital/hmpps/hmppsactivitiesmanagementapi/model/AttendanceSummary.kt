package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Attendance counts for a prisoner over a date range")
data class AttendanceSummary(
  @Schema(description = "Number of acceptable absences in the period", example = "2")
  val acceptableAbsence: Int = 0,

  @Schema(description = "Number of unacceptable absences in the period", example = "1")
  val unacceptableAbsence: Int = 0,

  @Schema(description = "Total number of attendances in the period which have a recorded outcome", example = "10")
  val total: Int = 0,
)
