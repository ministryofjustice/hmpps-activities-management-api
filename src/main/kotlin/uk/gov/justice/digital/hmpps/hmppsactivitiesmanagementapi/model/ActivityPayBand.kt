package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

// TODO swagger docs
data class ActivityPayBand(

  @Schema(description = "The internal ID for this activity pay band", example = "123456")
  val id: Long,

  val payBand: String? = null,

  val rate: Int? = null,

  val pieceRate: Int? = null,

  val pieceRateItems: Int? = null,
)
