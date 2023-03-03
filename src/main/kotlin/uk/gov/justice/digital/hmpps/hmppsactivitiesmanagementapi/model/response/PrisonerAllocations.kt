package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation

@Schema(description = "Describes a prisoners allocations")
data class PrisonerAllocations(

  @Schema(description = "The prisoner number", example = "GF10101")
  val prisonerNumber: String,

  @Schema(description = "The list of allocations for the prisoner")
  val allocations: List<Allocation>,
)
