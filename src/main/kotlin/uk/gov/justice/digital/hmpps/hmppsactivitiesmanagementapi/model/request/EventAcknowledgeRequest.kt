package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to acknowledge change events")
data class EventAcknowledgeRequest(
  @Schema(description = "The list of IDs to acknowledge", example = "[3,5,6]")
  val eventReviewIds: List<Long>,
)
