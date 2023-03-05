package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes one instance of a prison pay band")
data class PrisonPayBand(

  @Schema(description = "The internally-generated ID for this prison pay band", example = "123456")
  val id: Long,

  @Schema(description = "The order in which the pay band should be presented within a list e.g. dropdown", example = "1")
  val displaySequence: Int,

  @Schema(description = "The alternative text to use in place of the description e.g. Low, Medium, High", example = "Low")
  val alias: String,

  @Schema(description = "The description of pay band in this prison", example = "Pay band 1")
  val description: String,

  @Schema(description = "The pay band number this is associated with in NOMIS (1-10)", example = "1")
  val nomisPayBand: Int,

  @Schema(description = "The prison code for the pay band. Can also be 'DEFAULT' if none set up for prison", example = "MDI")
  val prisonCode: String,
)
