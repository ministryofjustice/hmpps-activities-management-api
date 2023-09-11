package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "An overview of attendance details for scheduled instances")
data class ScheduledInstanceAttendanceSummary(

  @Schema(
    description = "The id of the scheduled instance",
    example = "10",
  )
  val scheduledInstanceId: Long,

  @Schema(
    description = "The id of the activity",
    example = "1",
  )
  val activityId: Long,

  @Schema(
    description = "The id of the activity schedule",
    example = "2",
  )
  val activityScheduleId: Long,

  @Schema(
    description = "Summary of the activity",
    example = "Maths 1",
  )
  val summary: String,

  @Schema(
    description = "Category id of the activity",
    example = "2",
  )
  val categoryId: Long,

  @Schema(
    description = "The date of the scheduled instance",
    example = "2023-03-30",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val sessionDate: LocalDate,

  @Schema(description = "The start time of the scheduled instance", example = "09:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The start time of the scheduled instance", example = "09:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "Flag to indicate if the location of the activity is in cell", example = "false")
  var inCell: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  var onWing: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is off wing and not in a listed location", example = "false")
  var offWing: Boolean,

  @Schema(description = "The NOMIS internal location for this schedule")
  var internalLocation: InternalLocation? = null,

  @Schema(description = "Flag to indicate if the scheduled instance has been cancelled")
  var cancelled: Boolean,

  @Schema(description = "Attendance summary details for a scheduled instance")
  val attendanceSummary: AttendanceSummaryDetails,
)
