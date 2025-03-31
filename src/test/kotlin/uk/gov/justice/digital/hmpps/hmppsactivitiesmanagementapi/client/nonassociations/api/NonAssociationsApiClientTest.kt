package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociations.api

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
    nonAssociationsApiWebClient = NonAssociationsApiClient(webClient, RetryApiService(3, 250))
  }

  @Test
  fun `getOffenderNonAssociations - success`() {
    nonAssociationsApiMockServer.stubGetNonAssociations("A1143DZ")

    val result = nonAssociationsApiWebClient.getOffenderNonAssociations("A1143DZ")

    assertThat(result).extracting("id").containsOnly(83413L, 83511L)
  }

  @Test
  fun `getNonAssociationsInvolving - success when non-associations api succeeds`() {
    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("RSI")

    runBlocking {
      val result = nonAssociationsApiWebClient.getNonAssociationsInvolving("RSI", listOf("A22222A"))

      assertThat(result).extracting("id").containsOnly(1111L, 2222L, 3333L, 4444L)
    }
  }

  @Test
  fun `getNonAssociationsInvolving - success when non-associations api fails`() {
    nonAssociationsApiMockServer.stubGetNonAssociationsInvolvingError()

    runBlocking {
      val result = nonAssociationsApiWebClient.getNonAssociationsInvolving("RSI", listOf("A22222A"))

      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("Retrying failed api calls - connection reset")
  inner class RetryFailedCallsConnectionReset {

    @Test
    fun `will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
      nonAssociationsApiMockServer.stubGetNonAssociationsWithConnectionReset("A1143DZ")

      val result = nonAssociationsApiWebClient.getOffenderNonAssociations("A1143DZ")

      assertThat(result).extracting("id").containsOnly(83413L, 83511L)
    }

    @Test
    fun `will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
      nonAssociationsApiMockServer.stubGetNonAssociationsWithConnectionReset("A1143DZ", 2)

      val result = nonAssociationsApiWebClient.getOffenderNonAssociations("A1143DZ")

      assertThat(result).extracting("id").containsOnly(83413L, 83511L)
    }

    @Test
    fun `will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
      nonAssociationsApiMockServer.stubGetNonAssociationsWithConnectionReset("A1143DZ", 3)

      assertThrows<WebClientRequestException> {
        nonAssociationsApiWebClient.getOffenderNonAssociations("A1143DZ")
      }
    }
  }
}
