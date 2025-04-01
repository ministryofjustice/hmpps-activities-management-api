package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED

class NonAssociationsApiMockServer : MockServer(8555) {
  fun stubGetNonAssociations(prisonerNumber: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/prisoner/$prisonerNumber/non-associations"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("nonassociationsapi/$prisonerNumber-nonassociations.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetNonAssociationsInvolving(prisonCode: String) {
    stubFor(
      WireMock.post(WireMock.urlPathEqualTo("/non-associations/involving"))
        .withQueryParam("prisonId", matching(prisonCode))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("nonassociationsapi/non-associations-involving.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetNonAssociationsInvolvingError() {
    stubFor(
      WireMock.post(WireMock.urlPathEqualTo("/non-associations/involving"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubGetNonAssociationsWithConnectionReset(prisonerNumber: String, numFails: Int = 1) {
    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/prisoner/$prisonerNumber/non-associations"))
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
      WireMock.get("/prisoner/$prisonerNumber/non-associations")
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("nonassociationsapi/$prisonerNumber-nonassociations.json")
            .withStatus(200),
        ),
    )
  }
}
