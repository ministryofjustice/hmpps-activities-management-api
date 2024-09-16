package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

  @Schema(description = "The start time", example = "9:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time", example = "11:30")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "WAITING, COMPLETED.", example = "WAITING")
  val status: String,

  @Schema(description = "The reason for attending or not")
  val attendanceReasonCode: String? = null,

  @Schema(description = "Should payment be issued for SICK, REST or OTHER", example = "true")
  val issuePayment: Boolean?,

  @Schema(description = "The prisoner number for this attendance record", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The id of the particular session instance for this attendance record", example = "1")
  val scheduledInstanceId: Long,

  @Schema(description = "The id of the activity for this attendance record", example = "1")
  val activityId: Long,

  @Schema(description = "The title of the activity for this attendance record", example = "Math Level 1")
  val activitySummary: String,

  @Schema(description = "The name of the activity category for this attendance record", example = "Education")
  val categoryName: String,

  @Schema(description = "The date and time the attendance was updated", example = "2023-09-10T09:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val recordedTime: LocalDateTime?,

  @Schema(description = "Is attendance required?", example = "true")
  val attendanceRequired: Boolean,

  @Schema(description = "event tier")
  val eventTier: EventTierType?,
)
