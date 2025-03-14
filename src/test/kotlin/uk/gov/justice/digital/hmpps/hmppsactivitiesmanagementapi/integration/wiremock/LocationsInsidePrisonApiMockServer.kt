package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
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
      WireMock.get("/locations/$dpsLocationId?formatLocalName=true").willReturn(
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

  fun stubLocationsWithUsageTypes(
    prisonCode: String = "RSI",
    dpsLocationIds: Set<UUID> = setOf(UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")),
    locations: List<Location>? = null,
  ): List<Location> {
    val responseLocations = locations ?: defaultLocations(dpsLocationIds)

    stubFor(
      WireMock.get("/locations/prison/$prisonCode/non-residential-usage-type?formatLocalName=true").willReturn(
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
      WireMock.get("/locations/prison/$prisonCode/non-residential-usage-type/$usageType?formatLocalName=true").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(responseLocations))
          .withStatus(200),
      ),
    )
    return responseLocations
  }
}
