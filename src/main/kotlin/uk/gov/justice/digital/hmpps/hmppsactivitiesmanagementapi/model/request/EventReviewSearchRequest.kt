package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes the search filters to find change of circumstance events to be reviewed.")
data class EventReviewSearchRequest(
  @Schema(description = "The prison code where the events occurred", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The date on which events occurred")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val eventDate: LocalDate? = null,

  @Schema(description = "The specific prisoner number(s) to search in the events data", example = "G1234GH")
  val prisonerNumbers: List<String>? = null,

  @Schema(description = "The specific event(s) to search in the events data", example = "EVENT_CODE")
  val eventCodes: List<String>? = null,

  @Schema(description = "A boolean value indicating whether to filter for acknowledged events only. True returns acknowledged events only; false excludes acknowledged events. Default is false.", example = "true")
  val acknowledgedEvents: Boolean? = false,
)
