package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.location
import java.util.*

class LocationsInsidePrisonApiMockServer : MockServer(8093) {

  fun stubLocationFromDpsUuid(dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"), location: Location? = null): Location {
    val responseLocation = location ?: location()
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
}
