package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociationDetail

@Schema(description = "Prisoner workplace risk assessment suitability")
data class NonAssociationSuitability(
  @Schema(description = "The prisoner's suitability", example = "True")
  val suitable: Boolean,
  @Schema(description = "The prisoner's non-associations")
  val nonAssociations: List<OffenderNonAssociationDetail>,
)
