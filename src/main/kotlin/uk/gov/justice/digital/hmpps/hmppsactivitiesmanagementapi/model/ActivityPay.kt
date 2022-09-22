package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

// TODO swagger docs
data class ActivityPay(

  @Schema(description = "The internal ID for this activity pay", example = "123456")
  val id: Long,

  val bands: List<ActivityPayBand> = emptyList(),

  @Schema(description = "The incentive/earned privilege basic rate for this activity pay", example = "1.00")
  val iepBasicRate: Int? = null,

  @Schema(description = "The incentive/earned privilege standard rate for this activity pay", example = "1.25")
  val iepStandardRate: Int? = null,

  @Schema(description = "The incentive/earned privilege enhanced rate for this activity pay", example = "1.50")
  val iepEnhancedRate: Int? = null,

  val pieceRate: Int? = null,

  val pieceRateItems: Int? = null,
)
