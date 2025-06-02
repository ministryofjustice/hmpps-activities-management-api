package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "Describes one instance of a prison pay band")
data class PrisonPayBandCreateRequest(

  @field:Positive(message = "Display sequence must be supplied")
  @Schema(description = "The order in which the pay band should be presented within a list e.g. dropdown", example = "1")
  val displaySequence: Int,

  @field:NotEmpty(message = "Alias must be supplied")
  @field:Size(max = 30, message = "Alias should not exceed {max} characters")
  @Schema(description = "The alternative text to use in place of the description e.g. Low, Medium, High", example = "Low")
  val alias: String,

  @field:NotEmpty(message = "Payband description must be supplied")
  @field:Size(max = 100, message = "Description should not exceed {max} characters")
  @Schema(description = "The description of pay band in this prison", example = "Pay band 1")
  val description: String,

  @field:Positive(message = "Nomis pay band must be supplied")
  @Schema(description = "The pay band number this is associated with in NOMIS (1-10)", example = "1")
  val nomisPayBand: Int,
)
