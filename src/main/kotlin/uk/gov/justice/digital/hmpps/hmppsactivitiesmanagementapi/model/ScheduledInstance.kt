package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "Describes one instance of an activity schedule")
data class ScheduledInstance(

  @Schema(description = "The internally-generated ID for this scheduled instance", example = "123456")
  val id: Long,

  @Schema(description = "The specific date for this scheduled instance", example = "30/09/2022")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate,

  @Schema(description = "The start time for this scheduled instance", example = "9:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time for this scheduled instance", example = "10:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "Flag to indicate if this scheduled instance has been cancelled since being scheduled", example = "false")
  val cancelled: Boolean,

  @Schema(description = "Date and time this scheduled instance was cancelled (or null if not cancelled)", example = "29/09/2022 11:20")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val cancelledTime: LocalDateTime? = null,

  @Schema(description = "The person who cancelled this scheduled instance (or null if not cancelled)", example = "Adam Smith")
  val cancelledBy: String? = null,

  @Schema(description = "The attendance records for this scheduled instance")
  val attendances: List<Attendance> = emptyList()
)
