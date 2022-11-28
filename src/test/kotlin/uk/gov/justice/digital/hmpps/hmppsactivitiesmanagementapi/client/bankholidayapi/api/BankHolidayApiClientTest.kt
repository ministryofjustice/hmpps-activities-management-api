package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.bankholidayapi.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.BankHolidayApiMockServer
import java.time.LocalDate

class BankHolidayApiClientTest {

  private lateinit var bankHolidayApiClient: BankHolidayApiClient

  companion object {

    @JvmField
    internal val bankHolidayApiMockServer = BankHolidayApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      bankHolidayApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      bankHolidayApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    bankHolidayApiMockServer.resetRequests()

    val webClient = WebClient.create("http://localhost:${bankHolidayApiMockServer.port()}")
    bankHolidayApiClient = BankHolidayApiClient(webClient)
  }

  @Test
  fun `getBankHolidays - success`() {
    val bankHolidays = bankHolidayApiClient.getBankHolidays()
    assertThat(bankHolidays.englandAndWales.events).hasSize(1)
    assertThat(bankHolidays.englandAndWales.events.first().date).isEqualTo(LocalDate.now())
  }
}
