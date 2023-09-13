package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import java.time.LocalDate

@Schema(description = "Describes a candidate for allocation to an activity")
data class ActivityCandidate(
  @Schema(description = "The candidate's name", example = "Joe Bloggs")
  val name: String,

  @Schema(description = "The candidate's prisoner number", example = "GF10101")
  val prisonerNumber: String,

  @Schema(description = "The candidate's cell location", example = "MDI-1-1-101")
  val cellLocation: String?,

  @Schema(description = "Any activities the candidate is currently allocated to (excluding ended)")
  val otherAllocations: List<Allocation>,

  @Schema(description = "The candidate's earliest release date", example = "2027-01-24")
  val releaseDate: LocalDate?,
)
