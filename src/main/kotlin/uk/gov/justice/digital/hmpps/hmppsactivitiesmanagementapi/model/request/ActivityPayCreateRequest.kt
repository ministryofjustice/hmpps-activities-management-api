package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Positive

@Schema(description = "Describes the pay rates and bands to be created for an activity")
data class ActivityPayCreateRequest(

  @Schema(description = "The incentive/earned privilege level (nullable)", example = "BAS")
  val incentiveLevel: String? = null,

  @Schema(description = "The pay band (nullable)", example = "A")
  val payBand: String? = null,

  @field:Positive(message = "The earning rate must be a positive integer")
  @Schema(description = "The earning rate for one half day session for someone of this incentive level and pay band (in pence)", example = "150")
  val rate: Int? = null,

  @field:Positive(message = "The piece rate must be a positive integer")
  @Schema(description = "Where payment is related to produced amounts of a product, this indicates the payment rate (in pence) per pieceRateItems produced", example = "150")
  val pieceRate: Int? = null,

  @field:Positive(message = "The piece rate items must be a positive integer")
  @Schema(description = "Where payment is related to the number of items produced in a batch of a product, this is the batch size that attract 1 x pieceRate", example = "10")
  val pieceRateItems: Int? = null,
)
