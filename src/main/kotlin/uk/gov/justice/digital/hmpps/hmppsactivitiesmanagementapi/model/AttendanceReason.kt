package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class AttendanceReason(

  @Schema(description = "The internally-generated ID for this absence reason", example = "123456")
  val id: Long,

  @Schema(description = "The reason codes", example = "ABS, ACCAB, ATT, CANC, NREQ, SUS, UNACAB, REST")
  val code: String,

  @Schema(description = "The detailed description for this attendance reason", example = "Unacceptable absence")
  val description: String
)
