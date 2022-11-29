package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.api.BankHolidayApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.model.BankHoliday
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.model.BankHolidays
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.model.RegionBankHolidays
import java.time.LocalDate

class BankHolidayServiceTest {
  private val bankHolidayApiClient: BankHolidayApiClient = mock()

  private val service = BankHolidayService(bankHolidayApiClient)

  @Test
  fun `isEnglishBankHoliday returns true`() {
    whenever(bankHolidayApiClient.getBankHolidays()).thenReturn(bankHolidays)

    assertThat(service.isEnglishBankHoliday(LocalDate.now())).isTrue
  }

  @Test
  fun `isEnglishBankHoliday returns false`() {
    whenever(bankHolidayApiClient.getBankHolidays()).thenReturn(bankHolidays)

    assertThat(service.isEnglishBankHoliday(LocalDate.now().plusDays(1))).isFalse
  }

  companion object {
    val bankHolidays = BankHolidays(
      englandAndWales = RegionBankHolidays(
        division = "england-and-wales",
        events = listOf(
          BankHoliday(title = "Stubbed bank holiday", date = LocalDate.now())
        )
      )
    )
  }
}
