package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocalAuditRecord

@Schema(description = "The result of an audit record search")
data class LocalAuditSearchResults(

  @Schema(description = "The matching records")
  val content: List<LocalAuditRecord>,

  @Schema(description = "The current page number", example = "0")
  val pageNumber: Int,

  @Schema(description = "The total number of pages", example = "5")
  val totalPages: Int,
)
