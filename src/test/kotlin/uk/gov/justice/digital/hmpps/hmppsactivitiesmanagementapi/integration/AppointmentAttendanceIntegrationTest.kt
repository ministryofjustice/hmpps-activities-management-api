package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
    "feature.event.appointments.appointment-instance.updated=true",
    "feature.event.appointments.appointment-instance.deleted=true",
    "feature.event.appointments.appointment-instance.cancelled=true",
  ],
)
class AppointmentAttendanceIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var auditService: AuditService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @MockBean
  private lateinit var telemetryClient: TelemetryClient
  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @Test
  fun `mark appointment attendance authorisation required`() {
    webTestClient.patch()
      .uri("/appointments/1")
      .bodyValue(AppointmentAttendanceRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `update appointment by unknown id returns 404 not found`() {
    webTestClient.patch()
      .uri("/appointments/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .bodyValue(AppointmentAttendanceRequest())
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-id-1.sql",
  )
  @Test
  fun `mark attendance for past appointment`() {
    val request = AppointmentAttendanceRequest(
      attendedPrisonNumbers = listOf("B2345CD"),
      nonAttendedPrisonNumbers = listOf("C3456DE"),
    )

    val appointment = webTestClient.markAppointmentAttendance(1, request)!!

    with(appointment.attendees.single { it.prisonerNumber == "A1234BC" }) {
      assertThat(attended).isNull()
      assertThat(attendanceRecordedTime).isNull()
      assertThat(attendanceRecordedBy).isNull()
    }
    with(appointment.attendees.single { it.prisonerNumber == "B2345CD" }) {
      assertThat(attended).isTrue()
      assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(attendanceRecordedBy).isEqualTo("test-client")
    }
    with(appointment.attendees.single { it.prisonerNumber == "C3456DE" }) {
      assertThat(attended).isFalse()
      assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(attendanceRecordedBy).isEqualTo("test-client")
    }

    verifyNoInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("RSI")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointment.id.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isGreaterThan(0.0)
    }

    verifyNoMoreInteractions(telemetryClient)

    verifyNoInteractions(auditService)
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-id-1.sql",
  )
  @Test
  fun `mark attendance for future appointment`() {
    val request = AppointmentAttendanceRequest(
      attendedPrisonNumbers = listOf("C3456DE"),
      nonAttendedPrisonNumbers = listOf("B2345CD"),
    )

    val appointment = webTestClient.markAppointmentAttendance(3, request)!!

    with(appointment.attendees.single { it.prisonerNumber == "A1234BC" }) {
      assertThat(attended).isNull()
      assertThat(attendanceRecordedTime).isNull()
      assertThat(attendanceRecordedBy).isNull()
    }
    with(appointment.attendees.single { it.prisonerNumber == "B2345CD" }) {
      assertThat(attended).isFalse()
      assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(attendanceRecordedBy).isEqualTo("test-client")
    }
    with(appointment.attendees.single { it.prisonerNumber == "C3456DE" }) {
      assertThat(attended).isTrue()
      assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(attendanceRecordedBy).isEqualTo("test-client")
    }

    verifyNoInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("RSI")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointment.id.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY]).isEqualTo(0.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isGreaterThan(0.0)
    }

    verifyNoMoreInteractions(telemetryClient)

    verifyNoInteractions(auditService)
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-id-1.sql",
  )
  @Test
  fun `change attendance for appointment`() {
    val request = AppointmentAttendanceRequest(
      attendedPrisonNumbers = listOf("C3456DE"),
      nonAttendedPrisonNumbers = listOf("B2345CD"),
    )

    val currentAttendance = webTestClient.getAppointmentById(2)!!.attendees

    val appointment = webTestClient.markAppointmentAttendance(2, request)!!

    with(appointment.attendees.single { it.prisonerNumber == "A1234BC" }) {
      assertThat(attended).isNull()
      assertThat(attendanceRecordedTime).isNull()
      assertThat(attendanceRecordedBy).isNull()
    }
    with(currentAttendance.single { it.prisonerNumber == "B2345CD" }) {
      assertThat(attended).isTrue()
      assertThat(attendanceRecordedTime).isBefore(LocalDateTime.now().minusDays(1))
      assertThat(attendanceRecordedBy).isEqualTo("PREV.ATTENDANCE.RECORDED.BY")
    }
    with(appointment.attendees.single { it.prisonerNumber == "B2345CD" }) {
      assertThat(attended).isFalse()
      assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(attendanceRecordedBy).isEqualTo("test-client")
    }
    with(currentAttendance.single { it.prisonerNumber == "C3456DE" }) {
      assertThat(attended).isFalse()
      assertThat(attendanceRecordedTime).isBefore(LocalDateTime.now().minusDays(1))
      assertThat(attendanceRecordedBy).isEqualTo("PREV.ATTENDANCE.RECORDED.BY")
    }
    with(appointment.attendees.single { it.prisonerNumber == "C3456DE" }) {
      assertThat(attended).isTrue()
      assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(attendanceRecordedBy).isEqualTo("test-client")
    }

    verifyNoInteractions(eventsPublisher)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    telemetryPropertyMap.allValues hasSize 1
    telemetryMetricsMap.allValues hasSize 1

    with(telemetryPropertyMap.firstValue) {
      assertThat(this[USER_PROPERTY_KEY]).isEqualTo("test-client")
      assertThat(this[PRISON_CODE_PROPERTY_KEY]).isEqualTo("RSI")
      assertThat(this[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointment.id.toString())
    }

    with(telemetryMetricsMap.firstValue) {
      assertThat(this[PRISONERS_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(this[PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY]).isEqualTo(2.0)
      assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isGreaterThan(0.0)
    }

    verifyNoMoreInteractions(telemetryClient)

    verifyNoInteractions(auditService)
  }

  private fun WebTestClient.getAppointmentById(id: Long) =
    get()
      .uri("/appointments/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody

  private fun WebTestClient.markAppointmentAttendance(
    id: Long,
    request: AppointmentAttendanceRequest,
  ) =
    put()
      .uri("/appointments/$id/attendance")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody
}
