package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
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

    apiClient = LocationsInsidePrisonAPIClient(webClient, RetryApiService(3, 250))
  }

  @Test
  fun `should return location given a DPS location Uuid`() {
    val mockLocation = mockServer.stubLocationFromDpsUuid()

    val location = apiClient.getLocationById(dpsLocation().id)

    assertThat(location).isEqualTo(mockLocation)
  }

  @Test
  fun `should return non-residential locations`() {
    val mockLocations = mockServer.stubNonResidentialLocations("RSI")
    runBlocking {
      val locations = apiClient.getNonResidentialLocations("RSI")
      assertThat(locations).isEqualTo(mockLocations)
    }
  }

  @Test
  fun `should return locations for a usage type`() {
    val mockLocations = mockServer.stubLocationsForUsageType()

    runBlocking {
      val locations = apiClient.getLocationsForUsageType("RSI", NonResidentialUsageDto.UsageType.APPOINTMENT)
      assertThat(locations).isEqualTo(mockLocations)
    }
  }

  @Nested
  @DisplayName("Retrying failed api calls")
  inner class RetryFailedCalls {
    @Test
    fun `will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
      val mockLocation = mockServer.stubLocationFromDpsUuidWithConnectionReset()

      var location = apiClient.getLocationById(dpsLocation().id)

      assertThat(location).isEqualTo(mockLocation)
    }

    @Test
    fun `will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
      val mockLocation = mockServer.stubLocationFromDpsUuidWithConnectionReset(numFails = 2)

      var location = apiClient.getLocationById(dpsLocation().id)

      assertThat(location).isEqualTo(mockLocation)
    }

    @Test
    fun `will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
      mockServer.stubLocationFromDpsUuidWithConnectionReset(numFails = 3)

      assertThrows<WebClientRequestException> {
        apiClient.getLocationById(dpsLocation().id)
      }
    }
  }
}
