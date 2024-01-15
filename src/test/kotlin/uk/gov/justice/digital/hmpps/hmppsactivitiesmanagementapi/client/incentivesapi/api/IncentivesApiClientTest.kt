package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentives.api.IncentivesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.IncentivesApiMockServer

class IncentivesApiClientTest {
  private lateinit var incentivesApiClient: IncentivesApiClient

  companion object {
    @JvmField
    internal val incentivesApiMockServer = IncentivesApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      incentivesApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      incentivesApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    incentivesApiMockServer.resetAll()

    val webClient = WebClient.create("http://localhost:${incentivesApiMockServer.port()}")
    incentivesApiClient = IncentivesApiClient(webClient)
  }

  @Test
  fun `getIncentiveLevels - success`() {
    val prisonId = "MDI"

    incentivesApiMockServer.stubGetIncentiveLevels(prisonId)

    val incentiveLevels = incentivesApiClient.getIncentiveLevels(prisonId)

    assertThat(incentiveLevels).hasSize(5)
    assertThat(incentiveLevels.first().levelCode).isEqualTo("BAS")
    assertThat(incentiveLevels.first().levelName).isEqualTo("Basic")
  }

  @Test
  fun `getIncentiveLevelsCached - success`() {
    val prisonId = "MDI"

    incentivesApiMockServer.stubGetIncentiveLevels(prisonId)

    val incentiveLevels = incentivesApiClient.getIncentiveLevelsCached(prisonId)

    assertThat(incentiveLevels).hasSize(5)
    assertThat(incentiveLevels.first().levelCode).isEqualTo("BAS")
    assertThat(incentiveLevels.first().levelName).isEqualTo("Basic")
  }
}
