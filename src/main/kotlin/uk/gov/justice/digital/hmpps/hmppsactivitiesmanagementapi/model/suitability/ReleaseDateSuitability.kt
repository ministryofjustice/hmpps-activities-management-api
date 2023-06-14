package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Prisoner release date suitability")
data class ReleaseDateSuitability(
  @Schema(description = "The prisoner's suitability", example = "True")
  val suitable: Boolean,
  @Schema(description = "The prisoner's earliest release date", example = "medium")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val earliestReleaseDate: LocalDate?,
)
