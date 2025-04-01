package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.NomisMappingApiMockServer
import java.util.*

class RetryApiServiceTest {
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
  fun `connection reset - will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
    mockServer.stubMappingFromDpsUuidWithConnectionReset(dpsLocationId, nomisLocationId)

    val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `connection reset - will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
    mockServer.stubMappingFromDpsUuidWithConnectionReset(dpsLocationId, nomisLocationId, 2)

    val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `connection reset - will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
    mockServer.stubMappingFromDpsUuidWithConnectionReset(dpsLocationId, nomisLocationId, 3)

    assertThrows<WebClientRequestException> {
      apiClient.getLocationMappingByDpsId(dpsLocationId)
    }
  }

  @Test
  fun `bad gateway - will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
    mockServer.stubMappingFromDpsUuidWithBadGateway(dpsLocationId, nomisLocationId)

    val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `bad gateway -will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
    mockServer.stubMappingFromDpsUuidWithBadGateway(dpsLocationId, nomisLocationId, 2)

    val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `bad gateway -will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
    mockServer.stubMappingFromDpsUuidWithBadGateway(dpsLocationId, nomisLocationId, 3)

    assertThrows<WebClientResponseException.BadGateway> {
      apiClient.getLocationMappingByDpsId(dpsLocationId)
    }
  }
}
