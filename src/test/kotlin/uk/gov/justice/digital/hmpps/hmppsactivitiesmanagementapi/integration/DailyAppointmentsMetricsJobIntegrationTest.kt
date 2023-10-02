package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DELETED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent

class DailyAppointmentsMetricsJobIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  private val propertyMetricsCaptor = argumentCaptor<Map<String, Double>>()

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

    verify(telemetryClient, times(9)).trackEvent(eq(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value), any(), any())
  }

  @Sql(
    "classpath:test_data/seed-appointment-deleted-id-3.sql",
  )
  @Test
  fun `generate daily metrics with deleted appointments`() {
    prisonApiMockServer.stubGetAppointmentScheduleReasons()

    webTestClient.post()
      .uri("/job/appointments-metrics")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isCreated

    Thread.sleep(5000)

    verify(telemetryClient, times(9)).trackEvent(eq(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value), any(), propertyMetricsCaptor.capture())

    with(propertyMetricsCaptor.firstValue) {
      Assertions.assertThat(get(DELETED_APPOINTMENT_COUNT_METRIC_KEY)).isEqualTo(1.0)
    }
  }
}
