package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendanceMarkedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import java.time.LocalDateTime

class AppointmentTransactionalEventListenerTest {
  private val telemetryClient: TelemetryClient = mock()

  private val listener = AppointmentTransactionalEventListener(telemetryClient)

  private val logMessage = argumentCaptor<String>()
  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @Nested
  @DisplayName("handle appointment attendance marked event")
  inner class HandleAppointmentAttendanceMarkedEvent {
    private val event = AppointmentAttendanceMarkedEvent(
      appointmentId = 1,
      prisonCode = MOORLAND_PRISON_CODE,
      attendedPrisonNumbers = mutableListOf("B2345CD"),
      nonAttendedPrisonNumbers = mutableListOf("A1234BC", "C3456DE"),
      attendanceChangedPrisonNumbers = mutableListOf("B2345CD", "A1234BC", "C3456DE"),
      attendanceRecordedTime = LocalDateTime.now().minusSeconds(10),
      attendanceRecordedBy = "ATTENDANCE.RECORDED.BY",
    )

    @Test
    fun `tracks custom event`() {
      listener.handleAppointmentAttendanceMarkedEvent(event)

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryPropertyMap.firstValue) {
        assertThat(this[USER_PROPERTY_KEY]).isEqualTo(event.attendanceRecordedBy)
        assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo(MOORLAND_PRISON_CODE)
        assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isGreaterThanOrEqualTo(event.appointmentId.toString())
      }

      with(telemetryMetricsMap.firstValue) {
        assertThat(this[PRISONERS_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(event.attendedPrisonNumbers.size.toDouble())
        assertThat(this[PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(event.nonAttendedPrisonNumbers.size.toDouble())
        assertThat(this[PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY]).isEqualTo(event.attendanceChangedPrisonNumbers.size.toDouble())
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isGreaterThanOrEqualTo(10000.0)
      }
    }
  }
}
