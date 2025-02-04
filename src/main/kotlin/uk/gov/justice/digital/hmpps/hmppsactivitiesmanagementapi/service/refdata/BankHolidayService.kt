package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.api.BankHolidayApiClient
import java.time.LocalDate

@Service
class BankHolidayService(
  private val bankHolidayApiClient: BankHolidayApiClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun isEnglishBankHoliday(day: LocalDate) = bankHolidayApiClient.getBankHolidays().englandAndWales.events
    .any { it.date == day }
    .also { truthy ->
      if (truthy) {
        log.info("$day is a bank holiday")
      }
    }
}
