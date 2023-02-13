package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson

class PrisonerSearchApiMockServer : WireMockServer(8111) {

  fun stubSearchByPrisonerNumber(prisonerNumber: String) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(equalToJson("{\"prisonerNumbers\": [\"${prisonerNumber}\"]}", true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonersearchapi/prisoner-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubSearchByPrisonerNumberNotFound(prisonerNumber: String) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(equalToJson("{\"prisonerNumbers\": [\"${prisonerNumber}\"]}", true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("[]")
            .withStatus(200)
        )
    )
  }
}