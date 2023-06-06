package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Prisoner release date suitability")
data class ReleaseDateSuitability(
  @Schema(description = "The prisoner's suitability", example = "True")
  val suitable: Boolean,
  @Schema(description = "The prisoner's earliest release date", example = "medium")
  val earliestReleaseDate: LocalDate?,
)
