package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

data class PayHistoryMigrateResponse(
  val payRateDataSize: Long,
  val payHistoryDataSize: Long,
  val message: String
)
