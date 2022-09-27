package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the pay rates and bands which apply to an activity")
data class ActivityPay(

  @Schema(description = "The internally-generated ID for this activity pay", example = "123456")
  val id: Long,

  @Schema(description = "A list of pay bands and rates which apply to this activity pay. Can be empty if pay bands do not apply.")
  val bands: List<ActivityPayBand> = emptyList(),

  @Schema(description = "The incentive/earned privilege basic rate per schedule for this activity", example = "100")
  val iepBasicRate: Int? = null,

  @Schema(description = "The incentive/earned privilege standard rate per schedule for this activity", example = "125")
  val iepStandardRate: Int? = null,

  @Schema(description = "The incentive/earned privilege enhanced rate per schedule for this activity", example = "150")
  val iepEnhancedRate: Int? = null,

  @Schema(description = "Where payment is related to produced amounts of a product, this indicates the payment rate per pieceRateItems produced", example = "150")
  val pieceRate: Int? = null,

  @Schema(description = "Where payment is related to the number of items produced in a batch of a product, this is the batch size that attract 1 x pieceRate", example = "10")
  val pieceRateItems: Int? = null,
)
