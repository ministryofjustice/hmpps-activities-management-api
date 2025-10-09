package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.ManageAdjudicationsApiMockServer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ManageAdjudicationsApiFacadeTest {
  private lateinit var manageAdjudicationsApiFacade: ManageAdjudicationsApiFacade

  companion object {
    @JvmField
    internal val manageAdjudicationsApiMockServer = ManageAdjudicationsApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      manageAdjudicationsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      manageAdjudicationsApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    manageAdjudicationsApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${manageAdjudicationsApiMockServer.port()}")
    manageAdjudicationsApiFacade = ManageAdjudicationsApiFacade(webClient, RetryApiService(3, 250))
  }

  private val agencyId = "PVI"
  private val date = LocalDate.of(2025, 3, 24)
  val hearingSummary = HearingSummaryResponse(
    listOf(
      HearingSummaryDto(
        123,
        LocalDateTime.of(2025, 3, 24, 10, 0),
        LocalDateTime.of(2025, 3, 23, 11, 34),
        "chargeNumber 1",
        "G4793VF",
        "oicHearingType 2",
        "status 3",
        UUID.randomUUID(),
      ),
    ),
  )

  @Test
  fun `will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
    manageAdjudicationsApiMockServer.stubHearingsForDateWithConnectionReset(agencyId, date, hearingSummary)

    assertThat(manageAdjudicationsApiFacade.getAdjudicationHearingsForDate(agencyId, date)).isEqualTo(hearingSummary)
  }

  @Test
  fun `will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
    manageAdjudicationsApiMockServer.stubHearingsForDateWithConnectionReset(agencyId, date, hearingSummary, 2)

    assertThat(manageAdjudicationsApiFacade.getAdjudicationHearingsForDate(agencyId, date)).isEqualTo(hearingSummary)
  }

  @Test
  fun `will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
    manageAdjudicationsApiMockServer.stubHearingsForDateWithConnectionReset(agencyId, date, hearingSummary, 3)

    assertThrows<WebClientRequestException> {
      manageAdjudicationsApiFacade.getAdjudicationHearingsForDate(agencyId, date)
    }
  }
}
