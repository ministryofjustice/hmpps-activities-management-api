package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
  description =
  """
  Represents the key data required to report on attendance 
  """,
)
data class AllAttendance(
  @Schema(
    description = "The attendance primary key",
    example = "123456",
  )
  val attendanceId: Long,

  @Schema(
    description = "The scheduled instance date",
    example = "2023-03-30",
  )
  val sessionDate: LocalDate,

  @Schema(description = "AM, PM, ED.", example = "AM")
  val timeSlot: String,

  @Schema(description = "WAITING, COMPLETED, LOCKED.", example = "WAITING")
  val status: String,

  @Schema(description = "The reason for attending or not")
  val attendanceReasonCode: String? = null,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,
)
