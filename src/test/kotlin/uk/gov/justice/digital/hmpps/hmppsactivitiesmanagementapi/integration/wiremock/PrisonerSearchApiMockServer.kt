package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerNumbers

class PrisonerSearchApiMockServer : MockServer(8111) {

  fun stubGetAllPrisonersInPrison(prisonCode: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/prison/$prisonCode/prisoners?size=2000"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonersearchapi/all-prisoners.json")
            .withStatus(200),
        ),
    )
  }

  fun stubSearchByPrisonerNumbers(vararg prisonerNumber: String) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbers(prisonerNumbers = prisonerNumber.asList())), true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonersearchapi/prisoner-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubSearchByPrisonerNumbers(prisonerNumbers: List<String>, prisoners: List<Prisoner>) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbers(prisonerNumbers = prisonerNumbers)), true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisoners))
            .withStatus(200),
        ),
    )
  }

  fun stubSearchByPrisonerNumberNotFound(prisonerNumber: String) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbers(prisonerNumbers = listOf(prisonerNumber))), true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("[]")
            .withStatus(200),
        ),
    )
  }

  fun stubSearchByPrisonerNumber(prisonerNumber: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/prisoner/$prisonerNumber"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonersearchapi/prisoner-$prisonerNumber.json")
            .withStatus(200),
        ),
    )
  }

  fun stubSearchByPrisonerNumber(prisoner: Prisoner) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/prisoner/${prisoner.prisonerNumber}"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisoner))
            .withStatus(200),
        ),
    )
  }

  fun stubSearchByPrisonerNumberWithConnectionReset(prisonerNumber: String, numFails: Int = 1) {
    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/prisoner/$prisonerNumber"))
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
      WireMock.get(WireMock.urlEqualTo("/prisoner/$prisonerNumber"))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonersearchapi/prisoner-$prisonerNumber.json")
            .withStatus(200),
        ),
    )
  }
}
