package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Request object for updating an attendance record")
data class AttendanceUpdateRequest(

  @field:NotEmpty(message = "Attendance ID must be supplied")
  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long?,

  @field:NotEmpty(message = "Attendance reason must be supplied")
  @Schema(description = "The reason codes- SICK, REFUSED, NREQ, REST, CLASH, OTHER, SUSP, CANC, ATT", example = "ATT")
  val attendanceReason: String?,

  @Schema(description = "Comments such as more detail for SICK", example = "Prisoner has COVID-19")
  val comment: String?,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "Case note provided for REFUSED", example = "Prisoner refused to attend the scheduled activity without reasonable excuse")
  val caseNote: String?,

  @Schema(description = "Was an incentive level warning issued for REFUSED", example = "true")
  val incentiveLevelWarningIssued: Boolean?,

  @Schema(description = "The absence reason for OTHER", example = "Prisoner has another reason for missing the activity")
  val otherAbsenceReason: String?,
)
