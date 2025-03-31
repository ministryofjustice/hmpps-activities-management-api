package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingSummaryResponse
import java.time.LocalDate

class ManageAdjudicationsApiMockServer : MockServer(8777) {

  fun stubHearingsForDate(
    agencyId: String,
    date: LocalDate,
    body: String,
  ) {
    stubFor(
      WireMock.get(
        WireMock.urlEqualTo(
          "/reported-adjudications/hearings?hearingDate=$date",
        ),
      )
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader("Active-Caseload", agencyId)
            .withBody(body)
            .withStatus(200),
        ),
    )
  }

  fun stubHearings(
    agencyId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    prisoners: List<String>,
    body: String,
  ) {
    stubFor(
      WireMock.post(
        WireMock.urlEqualTo(
          "/reported-adjudications/hearings/$agencyId?startDate=$startDate&endDate=$endDate",
        ),
      )
        .withRequestBody(equalToJson(mapper.writeValueAsString(prisoners), true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(200),
        ),
    )
  }

  fun stubHearingsForDateWithConnectionReset(
    agencyId: String,
    date: LocalDate,
    hearingSummary: HearingSummaryResponse,
    numFails: Int = 1,
  ) {
    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/reported-adjudications/hearings?hearingDate=$date"))
          .inScenario("Network fail")
          .whenScenarioStateIs(if (i == 1) STARTED else "Fail ${i - 1}")
          .willReturn(
            WireMock.aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER),
          )
          .willSetStateTo("Fail $i"),
      )
    }

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/reported-adjudications/hearings?hearingDate=$date"))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader("Active-Caseload", agencyId)
            .withBody(mapper.writeValueAsString(hearingSummary))
            .withStatus(200),
        ),
    )
  }
}
