package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
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
        .withRequestBody(equalToJson(mapper.writeValueAsString(prisoners)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(200),
        ),
    )
  }
}
