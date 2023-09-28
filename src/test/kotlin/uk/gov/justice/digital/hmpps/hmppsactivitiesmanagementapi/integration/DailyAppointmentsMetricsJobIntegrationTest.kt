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

class DailyAppointmentsMetricsJobIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @Sql(
    "classpath:test_data/seed-appointment-set-id-6.sql",
  )
  @Test
  fun `generate daily metrics`() {
    prisonApiMockServer.stubGetAppointmentScheduleReasons()

    webTestClient.post()
      .uri("/job/appointments-metrics")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isCreated

    Thread.sleep(5000)

    verify(telemetryClient, times(6)).trackEvent(eq(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value), any(), any())
  }
}
