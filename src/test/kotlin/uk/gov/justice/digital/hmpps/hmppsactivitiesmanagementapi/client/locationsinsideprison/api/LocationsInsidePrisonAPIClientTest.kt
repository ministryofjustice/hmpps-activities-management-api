package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.LocationsInsidePrisonApiMockServer

class LocationsInsidePrisonAPIClientTest {
  private lateinit var apiClient: LocationsInsidePrisonAPIClient

  companion object {
    @JvmField
    internal val mockServer = LocationsInsidePrisonApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      mockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      mockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    mockServer.resetAll()

    val webClient = WebClient.create("http://localhost:${mockServer.port()}")

    apiClient = LocationsInsidePrisonAPIClient(webClient)
  }

  @Test
  fun `should return location given a DPS location Uuid`() {
    val mockLocation = mockServer.stubLocationFromDpsUuid()

    val location = apiClient.getLocationById(location().id)

    assertThat(location).isEqualTo(mockLocation)
  }

  @Test
  fun `should return locations with usage type`() {
    val mockLocations = mockServer.stubLocationsWithUsageTypes(prisonCode = "RSI")
    runBlocking {
      val locations = apiClient.getLocationsWithUsageTypes("RSI")
      assertThat(locations).isEqualTo(mockLocations)
    }
  }
}
