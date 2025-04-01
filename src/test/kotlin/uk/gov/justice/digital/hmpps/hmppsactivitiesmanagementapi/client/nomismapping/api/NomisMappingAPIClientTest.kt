package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.NomisMappingApiMockServer
import java.util.*

class NomisMappingAPIClientTest {
  private lateinit var apiClient: NomisMappingAPIClient

  val nomisLocationId = 1234L
  val dpsLocationId = UUID.randomUUID()

  companion object {
    @JvmField
    internal val mockServer = NomisMappingApiMockServer()

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

    apiClient = NomisMappingAPIClient(webClient, RetryApiService(3, 250))
  }

  @Test
  fun `should return a mapping given a NOMIS location ID`() {
    mockServer.stubMappingFromNomisId(nomisLocationId, dpsLocationId)

    val result = apiClient.getLocationMappingByNomisId(nomisLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `should return a mapping given a DPS location UUID`() {
    mockServer.stubMappingFromDpsUuid(dpsLocationId, nomisLocationId)

    val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `should return mappings given a set of NOMIS location IDs`() {
    val mappings = listOf(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))

    mockServer.stubMappingsFromNomisIds(mappings)

    runBlocking {
      val result = apiClient.getLocationMappingsByNomisIds(setOf(nomisLocationId))

      assertThat(result).isEqualTo(mappings)
    }
  }

  @Nested
  @DisplayName("Retrying failed api calls - connection reset")
  inner class RetryFailedCallsConnectionReset {

    @Test
    fun `will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
      mockServer.stubMappingFromDpsUuidWithConnectionReset(dpsLocationId, nomisLocationId)

      val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

      assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
    }

    @Test
    fun `will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
      mockServer.stubMappingFromDpsUuidWithConnectionReset(dpsLocationId, nomisLocationId, 2)

      val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

      assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
    }

    @Test
    fun `will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
      mockServer.stubMappingFromDpsUuidWithConnectionReset(dpsLocationId, nomisLocationId, 3)

      assertThrows<WebClientRequestException> {
        apiClient.getLocationMappingByDpsId(dpsLocationId)
      }
    }
  }
}
