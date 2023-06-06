package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education

@Schema(description = "Prisoner workplace education suitability")
data class EducationSuitability(
  @Schema(description = "The prisoner's suitability", example = "True")
  val suitable: Boolean,
  @Schema(description = "The prisoner's education levels")
  val education: List<Education>?,
)
