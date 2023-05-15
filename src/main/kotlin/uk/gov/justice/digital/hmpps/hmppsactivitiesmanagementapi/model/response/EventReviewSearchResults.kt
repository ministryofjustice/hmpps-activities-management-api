package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview

@Schema(description = "The result of an event review search")
data class EventReviewSearchResults(
  @Schema(description = "The matching records")
  val content: List<EventReview>,

  @Schema(description = "The current page number", example = "1")
  val pageNumber: Int,

  @Schema(description = "The total number of pages", example = "5")
  val totalPages: Int,
)
