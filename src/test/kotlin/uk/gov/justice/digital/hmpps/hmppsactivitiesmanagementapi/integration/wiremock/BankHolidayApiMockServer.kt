package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.LocalDate

class BankHolidayApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    @JvmField
    val bankHolidayApi = BankHolidayApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    bankHolidayApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    bankHolidayApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    bankHolidayApi.stop()
  }
}

class BankHolidayApiMockServer() : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8333
  }

  init {
    this.stubDayAsBankHoliday(LocalDate.now())
  }

  fun stubDayAsBankHoliday(day: LocalDate): StubMapping =
    stubFor(
      get("/bank-holidays.json")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
                 "england-and-wales":{
                    "division":"england-and-wales",
                    "events":[
                       {
                          "title":"Stubbed bank holiday",
                          "date":"$day"
                       }
                    ]
                 }
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
}
