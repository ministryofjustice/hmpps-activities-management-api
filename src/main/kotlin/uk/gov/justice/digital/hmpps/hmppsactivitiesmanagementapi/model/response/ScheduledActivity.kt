package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Schema(description = "Describes a scheduled activity for a prisoner")
data class ScheduledActivity(
  @Schema(description = "The internal ID of the scheduled instance", example = "1")
  val scheduledInstanceId: Long,

  @Schema(description = "The internal ID of the allocation", example = "1")
  val allocationId: Long,

  @Schema(description = "The prison code for this scheduled activity", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The specific date for this session", example = "2022-09-30")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val sessionDate: LocalDate,

  @Schema(description = "The start time for this scheduled instance", example = "09:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime? = null,

  @Schema(description = "The end time for this scheduled instance", example = "10:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime? = null,

  @Schema(description = "The prisoners prisoner number", example = "GF10101")
  val prisonerNumber: String,

  @Schema(description = "The booking id of the prisoner", example = "10001")
  val bookingId: Long,

  @Schema(description = "Set to true if this event will take place in the prisoner's cell", example = "false")
  val inCell: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  val onWing: Boolean,

  @Schema(description = "Flag to indicate if the location of the activity is off wing and not in a listed location", example = "false")
  val offWing: Boolean,

  @Schema(description = "The NOMIS internal location id where this event takes place", example = "10001")
  val internalLocationId: Int? = null,

  @Schema(description = "The optional DPS location UUID", example = "b7602cc8-e769-4cbb-8194-62d8e655992a")
  val dpsLocationId: UUID? = null,

  @Schema(description = "The NOMIS location code for this event", example = "5-A-SIDE COM")
  val internalLocationCode: String? = null,

  @Schema(description = "The NOMIS location description for this event", example = "MDI-GYM-5-A-SIDE COM")
  val internalLocationDescription: String? = null,

  val scheduleDescription: String? = null,

  @Schema(description = "The id of the activity", example = "1")
  val activityId: Int,

  @Schema(description = "The category for this activity, one of the high-level categories")
  val activityCategory: String,

  @Schema(description = "The title of the activity for this attendance record", example = "Math Level 1")
  val activitySummary: String? = null,

  @Schema(description = "Set to true if this event has been cancelled", example = "false")
  val cancelled: Boolean = false,

  @Schema(description = "Set to true if this prisoner is suspended from the scheduled event", example = "false")
  val suspended: Boolean = false,

  @Schema(description = "Set to true if this prisoner is auto-suspended from the scheduled event", example = "false")
  val autoSuspended: Boolean = false,

  @Schema(description = "Time slot of scheduled instance", example = "AM")
  val timeSlot: TimeSlot,

  @Schema(description = "Should activity payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "The activity attendance status - WAITING or COMPLETED", example = "WAITING")
  val attendanceStatus: AttendanceStatus?,

  @Schema(description = "The code for the activity (non) attendance reason", example = "SICK")
  val attendanceReasonCode: AttendanceReasonEnum?,

  @Schema(description = "Set to true if this activity is a paid activity", example = "false")
  val paidActivity: Boolean,

  val possibleAdvanceAttendance: Boolean,
)
