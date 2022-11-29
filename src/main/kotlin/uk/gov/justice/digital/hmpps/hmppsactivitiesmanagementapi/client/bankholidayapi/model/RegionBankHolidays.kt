package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.model

data class RegionBankHolidays(
  val division: String,
  val events: List<BankHoliday>,
)
