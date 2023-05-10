package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes one event that has occurred to indicate a potential change of circumstance")
data class EventReview(
  @Schema(description = "The internally-generated ID for this event", example = "123456")
  val eventReviewId: Long = 0,

  @Schema(description = "Describes the service which generated this event", example = "prisoner-offender-search")
  val serviceIdentifier: String? = null,

  @Schema(description = "The internal name for the event", example = "prisoner-offender-events.prisoner.cell-move")
  val eventType: String? = null,

  @Schema(description = "The date and time that this event occurred", example = "2022-10-01 23:11:01")
  val eventTime: LocalDateTime? = null,

  @Schema(description = "The prison code where this event took place", example = "MDI")
  val prisonCode: String? = null,

  @Schema(description = "The prisoner number which this event relates to", example = "G1234FF")
  val prisonerNumber: String? = null,

  @Schema(description = "The booking ID related to this prisoner", example = "123456")
  val bookingId: Int? = null,

  @Schema(
    description = "The description of the event that occurred",
    example = "The prisoner was moved to a different cell.",
  )
  val eventData: String? = null,

  @Schema(description = "The date and time that this event was acknowledged as seen.", example = "2022-10-01 23:11:01")
  val acknowledgedTime: LocalDateTime? = null,

  @Schema(description = "The username of the user who acknowledged the event.", example = "G4588F")
  val acknowledgedBy: String? = null,
)
