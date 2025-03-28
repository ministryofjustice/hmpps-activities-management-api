package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import java.util.*

class NomisMappingApiMockServer : MockServer(8094) {

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

  fun stubMappingsFromDpsIds(mappings: List<NomisDpsLocationMapping>) {
    stubFor(
      WireMock.post("/api/locations/dps").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(mappings))
          .withStatus(200),
      ),
    )
  }
}
