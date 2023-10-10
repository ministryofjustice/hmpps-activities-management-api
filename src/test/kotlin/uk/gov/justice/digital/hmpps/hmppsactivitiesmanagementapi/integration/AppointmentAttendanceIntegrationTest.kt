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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.risleyPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

  @MockBean
  private lateinit var telemetryClient: TelemetryClient
  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  @Test
  fun `get appointment attendance summary authorisation required`() {
    webTestClient.get()
      .uri("/appointments/RSI/2023-10-09")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance summary success`() {
    val prisonCode = risleyPrisonCode
    val date = LocalDate.now()

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
        appointmentCategoryReferenceCode("CHAP", "Chaplaincy"),
        appointmentCategoryReferenceCode("MEDO", "Medical - Doctor"),
      ),
    )

    prisonApiMockServer.stubGetLocationsForAppointments(
      prisonCode,
      listOf(
        appointmentLocation(123, prisonCode, userDescription = "Education 1"),
        appointmentLocation(456, prisonCode, userDescription = "Chapel"),
        appointmentLocation(789, prisonCode, userDescription = "Health Care Centre"),
      ),
    )

    val summaries = webTestClient.getAppointmentAttendanceSummaries(prisonCode, date)!!

    // Repeating group appointment, appointment id 2
    assertThat(summaries.single { it.id == 2L }).isEqualTo(
      AppointmentAttendanceSummary(
        2,
        prisonCode,
        "Education",
        AppointmentLocationSummary(123, prisonCode, "Education 1"),
        date,
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        false,
        6,
        3,
        2,
        1,
      ),
    )

    // Single appointments, ids 9-11
    // No attendance marked
    assertThat(summaries.single { it.id == 9L }).isEqualTo(
      AppointmentAttendanceSummary(
        9,
        prisonCode,
        "Jehovah's Witness One to One (Chaplaincy)",
        AppointmentLocationSummary(456, prisonCode, "Chapel"),
        date,
        LocalTime.of(13, 45),
        LocalTime.of(14, 15),
        false,
        1,
        0,
        0,
        1,
      ),
    )
    // Attended
    assertThat(summaries.single { it.id == 10L }).isEqualTo(
      AppointmentAttendanceSummary(
        10,
        prisonCode,
        "Jehovah's Witness One to One (Chaplaincy)",
        AppointmentLocationSummary(456, prisonCode, "Chapel"),
        date,
        LocalTime.of(14, 15),
        LocalTime.of(14, 45),
        false,
        1,
        1,
        0,
        0,
      ),
    )
    // Non-attended
    assertThat(summaries.single { it.id == 11L }).isEqualTo(
      AppointmentAttendanceSummary(
        11,
        prisonCode,
        "Jehovah's Witness One to One (Chaplaincy)",
        AppointmentLocationSummary(456, prisonCode, "Chapel"),
        date,
        LocalTime.of(14, 45),
        LocalTime.of(15, 15),
        false,
        1,
        0,
        1,
        0,
      ),
    )

    // Appointment set, returned as single appointments with ids 12-14
    assertThat(summaries.single { it.id == 12L }).isEqualTo(
      AppointmentAttendanceSummary(
        12,
        prisonCode,
        "Medical - Doctor",
        AppointmentLocationSummary(789, prisonCode, "Health Care Centre"),
        date,
        LocalTime.of(9, 0),
        LocalTime.of(9, 15),
        false,
        1,
        0,
        0,
        1,
      ),
    )
    // Attended
    assertThat(summaries.single { it.id == 13L }).isEqualTo(
      AppointmentAttendanceSummary(
        13,
        prisonCode,
        "Medical - Doctor",
        AppointmentLocationSummary(789, prisonCode, "Health Care Centre"),
        date,
        LocalTime.of(9, 15),
        LocalTime.of(9, 30),
        false,
        1,
        1,
        0,
        0,
      ),
    )
    // Non-attended
    assertThat(summaries.single { it.id == 14L }).isEqualTo(
      AppointmentAttendanceSummary(
        14,
        prisonCode,
        "Medical - Doctor",
        AppointmentLocationSummary(789, prisonCode, "Health Care Centre"),
        date,
        LocalTime.of(9, 30),
        LocalTime.of(9, 45),
        false,
        1,
        0,
        1,
        0,
      ),
    )

    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(telemetryClient)
    verifyNoInteractions(auditService)
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance summary for cancelled, deleted and removed statuses success`() {
    val prisonCode = risleyPrisonCode
    val date = LocalDate.now().minusDays(1)

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
        appointmentCategoryReferenceCode("CHAP", "Chaplaincy"),
        appointmentCategoryReferenceCode("MEDO", "Medical - Doctor"),
      ),
    )

    prisonApiMockServer.stubGetLocationsForAppointments(
      prisonCode,
      listOf(
        appointmentLocation(123, prisonCode, userDescription = "Education 1"),
        appointmentLocation(456, prisonCode, userDescription = "Chapel"),
        appointmentLocation(789, prisonCode, userDescription = "Health Care Centre"),
      ),
    )

    val summaries = webTestClient.getAppointmentAttendanceSummaries(prisonCode, date)!!

    // Repeating group appointment, appointment id 1
    assertThat(summaries.single { it.id == 1L }).isEqualTo(
      AppointmentAttendanceSummary(
        1,
        prisonCode,
        "Education",
        AppointmentLocationSummary(123, prisonCode, "Education 1"),
        date,
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        true,
        3,
        0,
        0,
        3,
      ),
    )
    // Repeating group appointment, appointment id 3 all attendees removed or deleted
    assertThat(summaries.singleOrNull { it.id == 3L }).isNull()

    // Single appointments, ids 4-8
    // Cancelled appointment
    assertThat(summaries.single { it.id == 4L }).isEqualTo(
      AppointmentAttendanceSummary(
        4,
        prisonCode,
        "Jehovah's Witness One to One (Chaplaincy)",
        AppointmentLocationSummary(456, prisonCode, "Chapel"),
        date,
        LocalTime.of(13, 45),
        LocalTime.of(14, 15),
        true,
        1,
        0,
        0,
        1,
      ),
    )
    // Deleted appointment
    assertThat(summaries.singleOrNull { it.id == 5L }).isNull()
    // No attendees
    assertThat(summaries.singleOrNull { it.id == 6L }).isNull()
    // No attendance marked and was removed
    assertThat(summaries.singleOrNull { it.id == 7L }).isNull()
    // No attendance marked and was deleted
    assertThat(summaries.singleOrNull { it.id == 8L }).isNull()

    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(telemetryClient)
    verifyNoInteractions(auditService)
  }

  @Test
  fun `mark appointment attendance authorisation required`() {
    webTestClient.patch()
      .uri("/appointments/1")
      .bodyValue(AppointmentAttendanceRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance.sql",
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
    "classpath:test_data/seed-appointment-attendance.sql",
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
    "classpath:test_data/seed-appointment-attendance.sql",
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

  private fun WebTestClient.getAppointmentAttendanceSummaries(
    prisonCode: String,
    date: LocalDate,
  ) =
    get()
      .uri("/appointments/$prisonCode/attendance-summaries?date=$date")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AppointmentAttendanceSummary::class.java)
      .returnResult().responseBody

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
