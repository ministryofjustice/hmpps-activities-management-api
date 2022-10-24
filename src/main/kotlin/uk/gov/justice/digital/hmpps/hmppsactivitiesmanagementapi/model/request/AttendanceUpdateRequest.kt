package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for updating an attendance record")
data class AttendanceUpdateRequest(

  @Schema(description = "The internally-generated ID for this attendance", example = "123456")
  val id: Long,

  @Schema(description = "The reason codes- ABS, ACCAB, ATT, CANC, NREQ, SUS, UNACAB, REST", example = "ATT")
  val attendanceReason: String
)
