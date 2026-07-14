package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDateTime

@Schema(description = "Represents a historical snapshot for an exclusion change.")
data class ExclusionRevision(
  @Schema(description = "The week number", example = "1")
  val weekNumber: Int,

  @Schema(description = "The time slots that were affected")
  val timeSlots: List<TimeSlot>,

  @Schema(description = "The day of the week", example = "MONDAY")
  val dayOfWeek: DayOfWeek,

  @Schema(description = "The type of revision", example = "ADDED")
  val revisionType: RevisionType,

  @Schema(description = "The revision number", example = "12345")
  val revision: Long,

  @Schema(description = "The username of the user that made the change", example = "BLOGGSJ")
  val updatedBy: String,

  @Schema(description = "The local date and time", example = "2026-06-25T10:15:30")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedDateTime: LocalDateTime,
)

enum class RevisionType { ADDED, REMOVED }
