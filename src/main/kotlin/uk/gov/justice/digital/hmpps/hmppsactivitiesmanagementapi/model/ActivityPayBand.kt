package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

// TODO swagger docs
data class ActivityPayBand(

  val id: Long,

  val payBand: String? = null,

  val rate: Int? = null,

  val pieceRate: Int? = null,

  val pieceRateItems: Int? = null,
)
