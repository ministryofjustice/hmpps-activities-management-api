package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(description = "Describes a top-level activity")
data class PrisonRegime(

  @Schema(description = "The internally-generated ID for this prison regime", example = "123456")
  val id: Long,

  @Schema(description = "The prison code where this activity takes place", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The start time for the am slot", example = "09:00")
  @JsonFormat(pattern = "HH:mm")
  val amStart: LocalTime,

  @Schema(description = "The end time for the am slot", example = "12:00")
  @JsonFormat(pattern = "HH:mm")
  val amFinish: LocalTime,

  @Schema(description = "The start time for the pm slot", example = "13:00")
  @JsonFormat(pattern = "HH:mm")
  val pmStart: LocalTime,

  @Schema(description = "The end time for the pm slot", example = "16:30")
  @JsonFormat(pattern = "HH:mm")
  val pmFinish: LocalTime,

  @Schema(description = "The start time for the ed slot", example = "18:00")
  @JsonFormat(pattern = "HH:mm")
  val edStart: LocalTime,

  @Schema(description = "The end time for the ed slot", example = "20:00")
  @JsonFormat(pattern = "HH:mm")
  val edFinish: LocalTime,

  @Schema(description = "days of week the regime is applicable to")
  val daysOfWeek: List<DayOfWeek>,
)
