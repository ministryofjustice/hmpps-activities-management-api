package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.model

import java.time.LocalDate

data class BankHoliday(
  val title: String,
  val date: LocalDate,
)
