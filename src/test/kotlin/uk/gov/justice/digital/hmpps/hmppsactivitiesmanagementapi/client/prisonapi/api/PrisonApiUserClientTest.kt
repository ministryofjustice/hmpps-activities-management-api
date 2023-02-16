package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonApiMockServer

class PrisonApiUserClientTest {
  private lateinit var prisonApiUserClient: PrisonApiUserClient

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    prisonApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${prisonApiMockServer.port()}")
    prisonApiUserClient = PrisonApiUserClient(webClient)
  }

  @Test
  fun `getUserCaseLoads - success`() {
    val prisonCode = "MDI"
    prisonApiMockServer.stubGetUserCaseLoads(prisonCode)
    val caseLoads = prisonApiUserClient.getUserCaseLoads().block()!!
    assertThat(caseLoads).hasSize(1)
    assertThat(caseLoads.first().caseLoadId).isEqualTo(prisonCode)
  }
}
