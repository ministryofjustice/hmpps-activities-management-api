package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import java.util.*

class LocationsInsidePrisonApiMockServer : MockServer(8093) {

  fun stubLocationFromDpsUuid(dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"), location: Location? = null): Location {
    val responseLocation = location ?: dpsLocation()
    stubFor(
      WireMock.get("/locations/$dpsLocationId?formatLocalName=true").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(responseLocation))
          .withStatus(200),
      ),
    )
    return responseLocation
  }

  fun stubLocationFromDpsUuidNotFound(dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")) {
    stubFor(
      WireMock.get("/locations/$dpsLocationId?formatLocalName=true&filterParents=false").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404),
      ),
    )
  }

  fun defaultLocations(dpsLocationIds: Set<UUID>): List<Location> {
    var i = 1

    return dpsLocationIds.map {
      dpsLocation(
        id = it,
        code = "CODE-$i",
        localName = "User Description ${i++}",
      )
    }
  }

  fun stubNonResidentialLocations(
    prisonCode: String = "RSI",
    dpsLocationIds: Set<UUID> = setOf(UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")),
    locations: List<Location>? = null,
  ): List<Location> {
    val responseLocations = locations ?: defaultLocations(dpsLocationIds)

    stubFor(
      WireMock.get("/locations/prison/$prisonCode/non-residential?formatLocalName=true").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(responseLocations))
          .withStatus(200),
      ),
    )
    return responseLocations
  }

  fun stubLocationsForUsageType(
    prisonCode: String = "RSI",
    usageType: UsageType = UsageType.APPOINTMENT,
    dpsLocationIds: Set<UUID> = setOf(UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")),
    locations: List<Location>? = null,
  ): List<Location> {
    val responseLocations = locations ?: defaultLocations(dpsLocationIds)

    stubFor(
      WireMock.get("/locations/prison/$prisonCode/non-residential-usage-type/$usageType?formatLocalName=true&filterParents=false").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(responseLocations))
          .withStatus(200),
      ),
    )
    return responseLocations
  }

  fun stubLocationFromDpsUuidWithConnectionReset(
    dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"),
    location: Location? = null,
    numFails: Int = 1,
  ): Location {
    val responseLocation = location ?: dpsLocation()

    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/locations/$dpsLocationId?formatLocalName=true"))
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
      WireMock.get(WireMock.urlEqualTo("/locations/$dpsLocationId?formatLocalName=true"))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(responseLocation))
            .withStatus(200),
        ),
    )

    return responseLocation
  }
}
