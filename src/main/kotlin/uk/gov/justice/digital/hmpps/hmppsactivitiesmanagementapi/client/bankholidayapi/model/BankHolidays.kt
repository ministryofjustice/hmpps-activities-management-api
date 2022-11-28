package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.model

import com.fasterxml.jackson.annotation.JsonProperty
data class BankHolidays(
  @JsonProperty("england-and-wales")
  val englandAndWales: RegionBankHolidays,
)
