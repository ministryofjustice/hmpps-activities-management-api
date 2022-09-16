package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

// TODO swagger docs
data class ActivityPay(

  val id: Long,

  val bands: List<ActivityPayBand> = emptyList(),

  val iepBasicRate: Int? = null,

  val iepStandardRate: Int? = null,

  val iepEnhancedRate: Int? = null,

  val pieceRate: Int? = null,

  val pieceRateItems: Int? = null,
)
