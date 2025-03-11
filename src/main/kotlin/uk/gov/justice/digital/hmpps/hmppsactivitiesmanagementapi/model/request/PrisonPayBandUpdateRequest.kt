package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

@Schema(description = "Describes one instance of a prison pay band")
data class PrisonPayBandUpdateRequest(
  @field:Positive(message = "Display sequence must be supplied")
  @Schema(description = "The order in which the pay band should be presented within a list e.g. dropdown", example = "1")
  val displaySequence: Int? = null,

  @field:NotEmpty(message = "Alias must be supplied")
  @Schema(description = "The alternative text to use in place of the description e.g. Low, Medium, High", example = "Low")
  val alias: String? = null,
)
