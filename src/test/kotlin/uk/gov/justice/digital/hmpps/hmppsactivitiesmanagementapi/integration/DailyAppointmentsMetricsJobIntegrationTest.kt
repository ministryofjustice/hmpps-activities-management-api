package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CANCELLED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DELETED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent

class DailyAppointmentsMetricsJobIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var telemetryClient: TelemetryClient
  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @BeforeEach
  fun setUp() {
    prisonApiMockServer.stubGetAppointmentScheduleReasons(
      listOf(
        appointmentCategoryReferenceCode("CHAP", "Chaplaincy"),
        appointmentCategoryReferenceCode("EDUC", "Education"),
        appointmentCategoryReferenceCode("IND", "Induction Meeting"),
      ),
    )
  }

  @Test
  fun `generate appointments metrics loops through each combination of prison code and category`() {
    webTestClient.generateAppointmentsMetrics()

    verify(telemetryClient, times(9)).trackEvent(eq(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

    assertThat(telemetryPropertyMap.allValues.map { Pair(it[PRISON_CODE_PROPERTY_KEY], it[CATEGORY_CODE_PROPERTY_KEY]) }).containsAll(
      listOf(
        Pair("PVI", "CHAP"),
        Pair("PVI", "EDUC"),
        Pair("PVI", "IND"),
        Pair("MDI", "CHAP"),
        Pair("MDI", "EDUC"),
        Pair("MDI", "IND"),
        Pair("RSI", "CHAP"),
        Pair("RSI", "EDUC"),
        Pair("RSI", "IND"),
      ),
    )

    verifyNoMoreInteractions(telemetryClient)
  }

  @Sql(
    "classpath:test_data/seed-appointments-daily-metrics.sql",
  )
  @Test
  fun `generate appointments metrics and verify counts`() {
    webTestClient.generateAppointmentsMetrics()

    verify(telemetryClient, times(9)).trackEvent(eq(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

    with(telemetryMetricsMap.firstValue) {
      this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 8
      this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 10
      this[APPOINTMENT_SERIES_COUNT_METRIC_KEY] isEqualTo 4
      this[APPOINTMENT_SET_COUNT_METRIC_KEY] isEqualTo 1
      this[CANCELLED_APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 2
      this[DELETED_APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 3
    }

    verifyNoMoreInteractions(telemetryClient)
  }

  private fun WebTestClient.generateAppointmentsMetrics() {
    post()
      .uri("/job/appointments-metrics")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isAccepted
    Thread.sleep(3000)
  }
}
