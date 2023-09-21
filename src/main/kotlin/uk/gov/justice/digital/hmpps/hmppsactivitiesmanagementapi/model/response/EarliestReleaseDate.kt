package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Summary of a prisoner's sentence and resulting earliest release date")
data class EarliestReleaseDate(
  @Schema(description = "The prisoner's earliest release date", example = "2027-09-20")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val releaseDate: LocalDate?,

  @Schema(description = "The prisoner's earliest release date is the tariff date")
  val isTariffDate: Boolean,

  @Schema(description = "The prisoner's sentence is indeterminate")
  val isIndeterminateSentence: Boolean,

  @Schema(description = "The prisoner is an immigration detainee")
  val isImmigrationDetainee: Boolean,

  @Schema(description = "The prisoner is convicted and unsentenced")
  val isConvictedUnsentenced: Boolean,

  @Schema(description = "The prisoner is on remand")
  val isRemand: Boolean,
)
