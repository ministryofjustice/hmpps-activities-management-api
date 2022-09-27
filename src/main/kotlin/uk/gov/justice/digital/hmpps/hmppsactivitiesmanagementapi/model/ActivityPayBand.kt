package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Activity pay band - used by some prisons to set a payment rate")
data class ActivityPayBand(

  @Schema(description = "The internally-generated ID for this activity pay band", example = "123456")
  val id: Long,

  @Schema(description = "The pay band code - usually A-F - to differentiate different pay rates", example = "A")
  val payBand: String? = null,

  @Schema(description = "The rate to be paid for one occurrence of this activity", example = "220")
  val rate: Int? = null,

  @Schema(description = "Where payment is related to produced amounts of a product, this indicates the payment rate per pieceRateItems produced", example = "150")
  val pieceRate: Int? = null,

  @Schema(description = "Where payment is related to the number of items produced in a batch of a product, this is the batch size that attract 1 x pieceRate", example = "10")
  val pieceRateItems: Int? = null,
)
