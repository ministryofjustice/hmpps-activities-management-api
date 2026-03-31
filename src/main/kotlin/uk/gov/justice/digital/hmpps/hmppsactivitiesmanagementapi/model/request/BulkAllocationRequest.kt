package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

//TODO: finish this

@Schema(description = "Request to allocate one or more prisoners to one or more schedules")
data class BulkAllocationRequest(
  @NotEmpty(message = "Schedule IDs must not be empty")
  @Schema(
    description = "List of schedule IDs to allocate prisoners to",
    example = "[1, 2, 3]",
    required = true,
  )
  val allocationRequests: List<PrisonerAllocationRequest>,

  @NotEmpty(message = "Allocation requests must not be empty")
  @Valid
  @Schema(
    description = "List of prisoner allocation requests",
    required = true,
  )
  val allocations: List<SchedulePrisonerAllocationRequest>,
)
