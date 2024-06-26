package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "suspended prisoner activity attendance")
data class SuspendedPrisonerAttendance(
  @Schema(description = "prisoner number")
  val prisonerNumber: String,
  @Schema(description = "attendance")
  val attendance: List<SuspendedPrisonerActivityAttendance>,
)

@Schema(description = "suspended prisoner activity attendance")
data class SuspendedPrisonerActivityAttendance(
  @Schema(description = "the activity start time")
  val startTime: LocalTime,
  @Schema(description = "the activity end time")
  val endTime: LocalTime,
  @Schema(description = "internal location description")
  var internalLocation: String? = null,
  @Schema(description = "Flag to indicate if the location of the activity is in cell", example = "false")
  var inCell: Boolean,
  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  var onWing: Boolean,
  @Schema(description = "Flag to indicate if the location of the activity is off wing and not in a listed location", example = "false")
  var offWing: Boolean,
  @Schema(description = "time slot")
  val timeSlot: String,
  @Schema(description = "category name")
  val categoryName: String,
  @Schema(description = "attendance reason code")
  val attendanceReasonCode: String,
)
