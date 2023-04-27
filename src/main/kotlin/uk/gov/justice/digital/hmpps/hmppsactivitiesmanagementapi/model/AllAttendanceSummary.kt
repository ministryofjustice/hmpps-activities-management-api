package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
  description =
  """
  Represents the key data required to report on daily attendance activity
  """,
)
data class AllAttendanceSummary(
  @Schema(
    description = "The attendance summary primary key",
    example = "123456",
  )
  val id: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The internally-generated ID for the activity", example = "123456")
  val activityId: Long,

  @Schema(description = "The name of the activity category", example = "Leisure and social")
  val categoryName: String,

  @Schema(
    description = "The scheduled instance date",
    example = "2023-03-30",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val sessionDate: LocalDate,

  @Schema(description = "AM, PM, ED.", example = "AM")
  val timeSlot: String,

  @Schema(description = "WAITING, COMPLETED, LOCKED.", example = "WAITING")
  val status: String,

  @Schema(description = "The reason for attending or not")
  val attendanceReasonCode: String? = null,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(
    description = "The number of attendance records",
    example = "123456",
  )
  val attendanceCount: Int,
)
