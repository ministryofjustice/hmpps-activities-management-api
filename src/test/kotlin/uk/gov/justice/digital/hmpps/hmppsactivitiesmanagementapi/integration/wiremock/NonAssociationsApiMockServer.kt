package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class NonAssociationsApiMockServer : WireMockServer(8555) {
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
}
