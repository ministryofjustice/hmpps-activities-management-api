package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
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

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(
    description = "The date of the session for which attendance may have been marked or a planned absence recorded",
    example = "2023-03-30",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val sessionDate: LocalDate,

  @Schema(description = "AM, PM, ED.", example = "AM")
  val timeSlot: String,

  @Schema(description = "WAITING, COMPLETED.", example = "WAITING")
  val status: String,

  @Schema(description = "The reason for attending or not")
  val attendanceReasonCode: String? = null,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "The prisoner number this attendance record is for", example = "A1234AA")
  val prisonerNumber: String,
)
