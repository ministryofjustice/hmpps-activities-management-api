package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class ActivityInstance(

  @Schema(description = "The internal ID for this activity instance", example = "123456")
  val id: Long,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val sessionDate: LocalDate,

  @Schema(description = "The start date and time for this activity instance", example = "30/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val startTime: LocalDateTime,

  @Schema(description = "The end date and time for this activity instance", example = "30/09/2022 10:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val endTime: LocalDateTime,

  @Schema(description = "Flag to indicated if this activity instance has been cancelled", example = "false")
  val cancelled: Boolean,

  @Schema(description = "Date and time if this activity instance has been cancelled", example = "29/09/2022 11:20")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val cancelledTime: LocalDateTime? = null,

  @Schema(description = "The person whom cancelled this activity instance", example = "Adam Smith")
  val cancelledBy: String? = null,
)
