package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class BulkPrisonerAllocationRequest(
  @Schema(description = "List of prisoner allocation requests")
  @field:NotEmpty(message = "Allocations must not be empty")
  @field:Valid
  val allocations: List<PrisonerAllocationRequest>,
)
