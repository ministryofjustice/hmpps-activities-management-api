package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes one instance of a planned suspension")
data class PlannedSuspension(
  @Schema(description = "The planned start date of the suspension", example = "2023-07-31")
  val plannedStartDate: LocalDate,

  @Schema(description = "The planned end date of the suspension", example = "2023-07-31")
  val plannedEndDate: LocalDate? = null,

  @Schema(description = "The optional case note identifier which was added to the prisoner's profile along with the suspension", example = "123456")
  val caseNoteId: Long? = null,

  @Schema(description = "The username of the person who planned the suspension", example = "ADMIN")
  val plannedBy: String,

  @Schema(description = "The system time when the suspension plan was made", example = "2023-07-10 14:54")
  val plannedAt: LocalDateTime,
)
