package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
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

    apiClient = NomisMappingAPIClient(webClient)
  }

  @Test
  fun `should return a DPS location UUID given a NOMIS location ID`() {
    mockServer.stubDpsUuidFromNomisId(nomisLocationId, dpsLocationId)

    val result = apiClient.getLocationMappingByNomisId(nomisLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }

  @Test
  fun `should return a NOMIS location ID given a DPS location UUID`() {
    mockServer.stubNomisIdFromDpsUuid(dpsLocationId, nomisLocationId)

    val result = apiClient.getLocationMappingByDpsId(dpsLocationId)

    assertThat(result).isEqualTo(NomisDpsLocationMapping(dpsLocationId, nomisLocationId))
  }
}
