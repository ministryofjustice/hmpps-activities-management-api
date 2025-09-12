package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import java.util.*

class NomisMappingApiMockServer : MockServer(8095) {

  fun stubMappingFromDpsUuid(dpsLocationId: UUID, nomisLocationId: Long = 1) {
    stubFor(
      WireMock.get("/api/locations/dps/$dpsLocationId").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(NomisDpsLocationMapping(dpsLocationId, nomisLocationId)))
          .withStatus(200),
      ),
    )
  }

  fun stubMappingFromNomisId(nomisLocationId: Long = 1234L, dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")) {
    stubFor(
      WireMock.get("/api/locations/nomis/$nomisLocationId").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(NomisDpsLocationMapping(dpsLocationId, nomisLocationId)))
          .withStatus(200),
      ),
    )
  }

  fun stubMappingFromNomisIdNotFound(nomisLocationId: Long = 1234L) {
    stubFor(
      WireMock.get("/api/locations/nomis/$nomisLocationId").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404),
      ),
    )
  }

  fun stubMappingsFromNomisIds(mappings: List<NomisDpsLocationMapping>) {
    stubFor(
      WireMock.post("/api/locations/nomis").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(mappings))
          .withStatus(200),
      ),
    )
  }

  fun stubMappingsFromDpsIds(mappings: List<NomisDpsLocationMapping>, dpsLocationIds: Set<UUID>? = null) {
    stubFor(
      WireMock.post("/api/locations/dps")
        .let { if (dpsLocationIds == null) it else it.withRequestBody(equalToJson(mapper.writeValueAsString(dpsLocationIds))) }
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(mappings))
            .withStatus(200),
        ),
    )
  }

  fun stubMappingFromDpsUuidWithConnectionReset(dpsLocationId: UUID, nomisLocationId: Long = 1, numFails: Int = 1) {
    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/api/locations/dps/$dpsLocationId"))
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
      WireMock.get(WireMock.urlEqualTo("/api/locations/dps/$dpsLocationId"))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(NomisDpsLocationMapping(dpsLocationId, nomisLocationId)))
            .withStatus(200),
        ),
    )
  }

  fun stubMappingFromDpsUuidWithBadGateway(dpsLocationId: UUID, nomisLocationId: Long = 1, numFails: Int = 1) {
    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/api/locations/dps/$dpsLocationId"))
          .inScenario("Network fail")
          .whenScenarioStateIs(if (i == 1) STARTED else "Fail ${i - 1}")
          .willReturn(
            WireMock.aResponse()
              .withStatus(502)
              .withHeader("Content-Type", "text/plain")
              .withBody("Bad Gateway"),
          )
          .willSetStateTo("Fail $i"),
      )
    }

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/locations/dps/$dpsLocationId"))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(NomisDpsLocationMapping(dpsLocationId, nomisLocationId)))
            .withStatus(200),
        ),
    )
  }
}
