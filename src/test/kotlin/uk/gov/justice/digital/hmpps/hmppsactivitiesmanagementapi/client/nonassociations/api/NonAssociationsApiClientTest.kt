package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociations.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.NonAssociationsApiMockServer

class NonAssociationsApiClientTest {
  private lateinit var nonAssociationsApiWebClient: NonAssociationsApiClient

  companion object {
    @JvmField
    internal val nonAssociationsApiMockServer = NonAssociationsApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      nonAssociationsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      nonAssociationsApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    nonAssociationsApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${nonAssociationsApiMockServer.port()}")
    nonAssociationsApiWebClient = NonAssociationsApiClient(webClient)
  }

  @Test
  fun `getOffenderNonAssociations - success`() {
    nonAssociationsApiMockServer.stubGetNonAssociations("A1143DZ")

    val result = nonAssociationsApiWebClient.getOffenderNonAssociations("A1143DZ")

    assertThat(result).extracting("id").containsOnly(83413L, 83511L)
  }

  @Test
  fun `getNonAssociationsInvolving - success`() {
    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("RSI")

    runBlocking {
      val result = nonAssociationsApiWebClient.getNonAssociationsInvolving("RSI", listOf("A22222A"))

      assertThat(result).extracting("id").containsOnly(1111L, 2222L)
    }
  }
}
