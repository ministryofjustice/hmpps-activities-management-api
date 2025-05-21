package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "An advance attendance record for a prisoner")
data class AdvanceAttendance(

  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long,

  @Schema(description = "The ID for scheduled instance for this attendance", example = "123456")
  val scheduleInstanceId: Long,

  @Schema(description = "The prison number this attendance record is for", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "The amount in pence to pay the prisoner for the activity based on today's date", example = "100")
  val payAmount: Int? = null,

  @Schema(description = "The date and time the attendance was updated", example = "2023-09-10T09:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime? = null,

  @Schema(description = "The person who updated the attendance", example = "A.JONES")
  val recordedBy: String? = null,

  @Schema(description = "The attendance history records for this attendance")
  val attendanceHistory: List<AdvanceAttendanceHistory>? = emptyList(),
)
