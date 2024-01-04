package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.AllocationPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.DeallocationCaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.NonAssociationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability

@Schema(description = "Cross references prisoners details with activity requirements")
data class AllocationSuitability(
  @Schema(description = "The prisoner's workplace risk assessment suitability")
  val workplaceRiskAssessment: WRASuitability? = null,

  @Schema(description = "The prisoner's incentive level suitability")
  val incentiveLevel: IncentiveLevelSuitability? = null,

  @Schema(description = "The prisoner's education suitability")
  val education: EducationSuitability? = null,

  @Schema(description = "The prisoner's release date suitability")
  val releaseDate: ReleaseDateSuitability? = null,

  @Schema(description = "The prisoner's non-association suitability")
  val nonAssociation: NonAssociationSuitability? = null,

  @Schema(description = "The prisoner's currently active allocations with pay rates")
  val allocations: List<AllocationPayRate> = emptyList(),

  @Schema(description = "The prisoner's previous allocations to this activity where they have been deallocated by a member of staff, with an optional case note")
  val previousDeallocations: List<DeallocationCaseNote> = emptyList(),
)
