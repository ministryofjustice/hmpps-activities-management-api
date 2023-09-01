package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonerSearchApiMockServer

class PrisonerSearchApiClientTest {
  private lateinit var prisonerSearchApiClient: PrisonerSearchApiClient

  companion object {
    @JvmField
    internal val prisonerSearchApiMockServer = PrisonerSearchApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonerSearchApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonerSearchApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    prisonerSearchApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${prisonerSearchApiMockServer.port()}")
    prisonerSearchApiClient = PrisonerSearchApiClient(webClient)
  }

  @Test
  fun `findByPrisonerNumbers - success`() {
    val prisonerNumber = "G4793VF"

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerNumber)).block()!!

    assertThat(prisoners).hasSize(1)
    assertThat(prisoners.first().prisonerNumber).isEqualTo(prisonerNumber)
  }

  @Test
  fun `findByPrisonerNumbers no numbers - success`() {
    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(emptyList()).block()!!

    assertThat(prisoners).hasSize(0)
  }

  @Test
  fun `findByPrisonerNumbersMap - success`() {
    val prisonerNumber = "G4793VF"

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbersMap(listOf(prisonerNumber))

    assertThat(prisonerMap).hasSize(1)
    with(prisonerMap[prisonerNumber]) {
      assertThat(this).isNotNull
      assertThat(this!!.prisonerNumber).isEqualTo(prisonerNumber)
    }
    assertThat(prisonerMap[prisonerNumber]).isNotNull
  }

  @Test
  fun `findByPrisonerNumbersAsync no numbers - success`() {
    runBlocking {
      val prisoners = prisonerSearchApiClient.findByPrisonerNumbersAsync(emptyList())
      assertThat(prisoners).hasSize(0)
    }
  }

  @Test
  fun `findByPrisonerNumber - success`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("G4793VF")

    with(prisonerSearchApiClient.findByPrisonerNumber("G4793VF")!!) {
      prisonerNumber isEqualTo "G4793VF"
    }
  }
}
