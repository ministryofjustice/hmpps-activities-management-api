package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matching

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
}
