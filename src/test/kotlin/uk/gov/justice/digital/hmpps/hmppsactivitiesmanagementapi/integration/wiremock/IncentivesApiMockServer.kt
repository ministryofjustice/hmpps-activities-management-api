package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class IncentivesApiMockServer : WireMockServer(8666) {

  fun stubGetIncentiveLevels(prisonId: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/incentive/prison-levels/$prisonId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("incentivesapi/incentive-levels-$prisonId.json")
            .withStatus(200),
        ),
    )
  }
}
