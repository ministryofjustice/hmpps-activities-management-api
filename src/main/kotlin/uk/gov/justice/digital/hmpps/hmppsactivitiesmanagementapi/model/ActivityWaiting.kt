package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ActivityWaiting(

  @Schema(description = "The internal ID for this activity waiting", example = "123456")
  val id: Long,

  @Schema(description = "The prison identifier for this activity waiting", example = "A1234AA")
  val prisonerNumber: String,

// TODO swagger docs
  val priority: Int,

  @Schema(description = "The date and time when this activity waiting was created", example = "01/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  @Schema(description = "The person whom created this activity waiting", example = "Adam Smith")
  val createdBy: String,
)
