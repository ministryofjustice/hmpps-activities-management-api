package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the capacity and allocated slots of an activity or category")
data class CapacityAndAllocated(
  @Schema(description = "The maximum number of people who can attend the category or activity", example = "30")
  val capacity: Int,

  @Schema(description = "The number of slots currently filled in the activity or category", example = "27")
  val allocated: Int
)
