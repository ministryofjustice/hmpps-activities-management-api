package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response for a successful migration of one allocation to an activity")
data class AllocationMigrateResponse(
  @Schema(description = "The actual activity ID to which the allocation was made", example = "123232")
  val activityId: Long,

  @Schema(description = "The allocation ID created in the activities service", example = "32323")
  val allocationId: Long,
)
