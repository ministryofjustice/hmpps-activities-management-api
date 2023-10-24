package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonerSearchApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture

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
    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerNumber))

    assertThat(prisoners).hasSize(1)
    assertThat(prisoners.first().prisonerNumber).isEqualTo(prisonerNumber)
  }

  @Test
  fun `findByPrisonerNumbers no numbers - success`() {
    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(emptyList())

    assertThat(prisoners).hasSize(0)
  }

  @Test
  fun `findByPrisonerNumbers batch requests - success`() {
    val prisonerNumbers = listOf("A1234BC", "B2345CD", "C3456DE", "D4567EF", "E5678FG")

    val batch1 = listOf(
      PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 1),
      PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", bookingId = 2),
    )
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(batch1.map { it.prisonerNumber }, batch1)
    val batch2 = listOf(
      PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", bookingId = 3),
      PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 4),
    )
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(batch2.map { it.prisonerNumber }, batch2)
    val batch3 = listOf(
      PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5678FG", bookingId = 5),
    )
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(batch3.map { it.prisonerNumber }, batch3)

    val batchSize = 2

    val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers, batchSize)

    assertThat(prisoners).hasSize(5)
    assertThat(prisoners.map { it.prisonerNumber }).isEqualTo(prisonerNumbers)
    assertThat(prisoners.map { it.bookingId }).isEqualTo(listOf("1", "2", "3", "4", "5"))
  }

  @Test
  fun `findByPrisonerNumbers batch size must be greater than zero`() {
    assertThatThrownBy {
      prisonerSearchApiClient.findByPrisonerNumbers(emptyList(), 0)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Batch size must be between 1 and 1000")
  }

  @Test
  fun `findByPrisonerNumbers batch size must be less than 1001`() {
    assertThatThrownBy {
      prisonerSearchApiClient.findByPrisonerNumbers(emptyList(), 1001)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Batch size must be between 1 and 1000")
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
