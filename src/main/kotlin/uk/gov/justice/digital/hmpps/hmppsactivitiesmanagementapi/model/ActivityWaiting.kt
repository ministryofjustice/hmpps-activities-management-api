package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes a person who is on a waiting list for an activity")
data class ActivityWaiting(

  @Schema(description = "The internally-generated ID for this data", example = "123456")
  val id: Long,

  @Schema(description = "The prisoner number (NomisId) of the person on the waiting list", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The priority of this person in the waiting list. The lower the number, the higher the priority", example = "1")
  val priority: Int,

  @Schema(description = "The date and time when this person was added to the waiting list", example = "01/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  @Schema(description = "The staff members name who added this person to the waiting list", example = "Adam Smith")
  val createdBy: String,
)
