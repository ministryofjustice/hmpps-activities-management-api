package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Represents the key data required to synchronise an attendance with Nomis 
  """,
)
data class AttendanceSync(
  @Schema(
    description = "The attendance primary key",
    example = "123456",
  )
  val attendanceId: Long,

  @Schema(
    description = "The scheduled instance primary key",
    example = "123456",
  )
  val scheduledInstanceId: Long,

  @Schema(
    description = "The activity schedule primary key",
    example = "123456",
  )
  val activityScheduleId: Long,

  @Schema(
    description = "The scheduled instance date",
    example = "2023-03-30",
  )
  val sessionDate: LocalDate,

  @Schema(
    description = "The scheduled instance start time",
    example = "10:00",
  )
  val sessionStartTime: LocalTime,

  @Schema(
    description = "The scheduled instance end time",
    example = "11:00",
  )
  val sessionEndTime: LocalTime,

  @Schema(
    description = "The offender's unique identifier",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @Schema(
    description = "The offender booking primary key",
    example = "123456",
  )
  val bookingId: Long,

  @Schema(description = "The reason for attending or not")
  val attendanceReasonCode: String? = null,

  @Schema(
    description = "Free text to allow comments to be put against the attendance",
    example = "Prisoner was too unwell to attend the activity.",
  )
  var comment: String? = null,

  @Schema(description = "WAITING, COMPLETED.", example = "WAITING")
  val status: String,

  @Schema(description = "The amount in pence to pay the prisoner for the activity", example = "100")
  val payAmount: Int? = null,

  @Schema(description = "The bonus amount in pence to pay the prisoner for the activity", example = "50")
  val bonusAmount: Int? = null,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,
)
