package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ActivityWaiting(

  @Schema(description = "The internal ID for this activity waiting", example = "123456")
  val id: Long,

  val prisonerNumber: String,

  val priority: Int,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  val createdBy: String,
)
