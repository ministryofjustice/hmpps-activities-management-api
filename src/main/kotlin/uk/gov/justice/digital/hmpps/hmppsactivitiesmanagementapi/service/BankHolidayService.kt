package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.api.BankHolidayApiClient
import java.time.LocalDate

@Service
class BankHolidayService(
  private val bankHolidayApiClient: BankHolidayApiClient,
) {
  fun isEnglishBankHoliday(day: LocalDate): Boolean {
    val bankHolidays = bankHolidayApiClient.getBankHolidays()

    return bankHolidays.englandAndWales.events.find { it.date == day } != null
  }
}
