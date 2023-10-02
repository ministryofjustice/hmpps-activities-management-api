package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent

class DailyActivityMetricsJobIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @Sql(
    "classpath:test_data/seed-activity-id-24.sql",
  )
  @Test
  fun `generate daily metrics`() {
    webTestClient.post()
      .uri("/job/activities-metrics")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isCreated

    Thread.sleep(5000)

    verify(telemetryClient, times(81)).trackEvent(eq(TelemetryEvent.ACTIVITIES_DAILY_STATS.value), any(), any())
  }
}
