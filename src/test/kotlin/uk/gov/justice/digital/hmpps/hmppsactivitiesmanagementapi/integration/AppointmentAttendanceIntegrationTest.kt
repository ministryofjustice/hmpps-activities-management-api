package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.MultipleAppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceAction
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceStatus
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
import java.util.*

class AppointmentAttendanceIntegrationTest : AppointmentsIntegrationTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  @MockitoBean
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

  private fun stubForAttendanceSummaries(prisonCode: String) {
    val dpsLocation1 = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), prisonCode)
    val dpsLocation2 = dpsLocation(UUID.fromString("22222222-2222-2222-2222-222222222222"), prisonCode)
    val dpsLocation3 = dpsLocation(UUID.fromString("33333333-3333-3333-3333-333333333333"), prisonCode)

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = prisonCode,
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation1, dpsLocation2, dpsLocation3),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation1.id, 123),
        NomisDpsLocationMapping(dpsLocation2.id, 456),
        NomisDpsLocationMapping(dpsLocation3.id, 789),
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-by-status.sql",
  )
  @EnumSource(AttendanceStatus::class)
  @ParameterizedTest
  fun `get appointment attendance by status`(status: AttendanceStatus) {
    stubForAttendanceSummaries(RISLEY_PRISON_CODE)
    val incLudeEventTier = if (status == AttendanceStatus.EVENT_TIER) "&eventTier=${EventTierType.TIER_1}" else ""
    val organiserCode = if (status == AttendanceStatus.EVENT_TIER) "&organiserCode=PRISON_STAFF" else ""
    webTestClient.get()
      .uri("/appointments/$RISLEY_PRISON_CODE/${status.name}/attendance?date=${LocalDate.now().minusDays(1)}$incLudeEventTier$organiserCode")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-by-status.sql",
  )
  @Test
  fun `get appointment attendance by prisoner, custom name and category code`() {
    stubForAttendanceSummaries(RISLEY_PRISON_CODE)
    webTestClient.get()
      .uri("/appointments/$RISLEY_PRISON_CODE/${AttendanceStatus.ATTENDED}/attendance?date=${LocalDate.now().minusDays(1)}&prisonerNumber=B2345CD&customName=CusTom&categoryCode=EDUC")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance with custom name filter`() {
    stubForAttendanceSummaries(RISLEY_PRISON_CODE)

    webTestClient.getAppointmentAttendanceSummaries(
      prisonCode = RISLEY_PRISON_CODE,
      date = LocalDate.now(),
      additionalFilters = "&customName=CuStom",
    )!!
      .all { it.appointmentName.contains("custom") }
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance with category code filter`() {
    stubForAttendanceSummaries(RISLEY_PRISON_CODE)

    webTestClient.getAppointmentAttendanceSummaries(
      prisonCode = RISLEY_PRISON_CODE,
      date = LocalDate.now(),
      additionalFilters = "&categoryCode=EDUC",
    )!!
      .all { it.appointmentName.contains("Education") }
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance with category code and custom name filter`() {
    stubForAttendanceSummaries(RISLEY_PRISON_CODE)

    webTestClient.getAppointmentAttendanceSummaries(
      prisonCode = RISLEY_PRISON_CODE,
      date = LocalDate.now(),
      additionalFilters = "&categoryCode=EDUC&customName=custom",
    )!!
      .all { it.appointmentName.contains("Education") && it.appointmentName.contains("custom") }
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance summary success`() {
    val prisonCode = RISLEY_PRISON_CODE
    val date = LocalDate.now()

    val dpsLocation1 = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), prisonCode, localName = "Education 1")
    val dpsLocation2 = dpsLocation(UUID.fromString("22222222-2222-2222-2222-222222222222"), prisonCode, localName = "Chapel")
    val dpsLocation3 = dpsLocation(UUID.fromString("33333333-3333-3333-3333-333333333333"), prisonCode, localName = "Health Care Centre")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = prisonCode,
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation1, dpsLocation2, dpsLocation3),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation1.id, 123),
        NomisDpsLocationMapping(dpsLocation2.id, 456),
        NomisDpsLocationMapping(dpsLocation3.id, 789),
      ),
    )

    val summaries = webTestClient.getAppointmentAttendanceSummaries(prisonCode, date)!!

    assertThat(summaries).containsAll(
      listOf(
        // Repeating group appointment, appointment id 2
        AppointmentAttendanceSummary(
          2,
          prisonCode,
          "custom (Education)",
          AppointmentLocationSummary(123, dpsLocation1.id, prisonCode, "Education 1"),
          false,
          date,
          LocalTime.of(9, 0),
          LocalTime.of(10, 30),
          false,
          6,
          3,
          2,
          1,
          listOf(
            AppointmentAttendeeSearchResult(4, "A1234BC", 1),
            AppointmentAttendeeSearchResult(5, "B2345CD", 2),
            AppointmentAttendeeSearchResult(6, "C3456DE", 3),
            AppointmentAttendeeSearchResult(7, "D4567EF", 4),
            AppointmentAttendeeSearchResult(8, "E5678FG", 5),
            AppointmentAttendeeSearchResult(9, "F6789GH", 6),
          ),
          EventTierType.TIER_1,
        ),
        // Single appointments, ids 9-11
        // No attendance marked
        AppointmentAttendanceSummary(
          9,
          prisonCode,
          "Jehovah's Witness One to One (Chaplaincy)",
          AppointmentLocationSummary(456, dpsLocation2.id, prisonCode, "Chapel"),
          false,
          date,
          LocalTime.of(13, 45),
          LocalTime.of(14, 15),
          false,
          1,
          0,
          0,
          1,
          listOf(
            AppointmentAttendeeSearchResult(17, "A1234BC", 1),
          ),
          EventTierType.TIER_1,
        ),
        // Attended
        AppointmentAttendanceSummary(
          10,
          prisonCode,
          "Jehovah's Witness One to One (Chaplaincy)",
          AppointmentLocationSummary(456, dpsLocation2.id, prisonCode, "Chapel"),
          false,
          date,
          LocalTime.of(14, 15),
          LocalTime.of(14, 45),
          false,
          1,
          1,
          0,
          0,
          listOf(AppointmentAttendeeSearchResult(18, "B2345CD", 2)),
          EventTierType.TIER_1,
        ),
        // Non-attended
        AppointmentAttendanceSummary(
          11,
          prisonCode,
          "Jehovah's Witness One to One (Chaplaincy)",
          AppointmentLocationSummary(456, dpsLocation2.id, prisonCode, "Chapel"),
          false,
          date,
          LocalTime.of(14, 45),
          LocalTime.of(15, 15),
          false,
          1,
          0,
          1,
          0,
          listOf(AppointmentAttendeeSearchResult(19, "C3456DE", 3)),
          EventTierType.TIER_1,
        ),
        // Appointment set, returned as single appointments with ids 12-14
        AppointmentAttendanceSummary(
          12,
          prisonCode,
          "Medical - Doctor",
          AppointmentLocationSummary(789, dpsLocation3.id, prisonCode, "Health Care Centre"),
          false,
          date,
          LocalTime.of(9, 0),
          LocalTime.of(9, 15),
          false,
          1,
          0,
          0,
          1,
          listOf(AppointmentAttendeeSearchResult(20, "A1234BC", 1)),
          EventTierType.TIER_1,
        ),
        // Attended
        AppointmentAttendanceSummary(
          13,
          prisonCode,
          "Medical - Doctor",
          AppointmentLocationSummary(789, dpsLocation3.id, prisonCode, "Health Care Centre"),
          false,
          date,
          LocalTime.of(9, 15),
          LocalTime.of(9, 30),
          false,
          1,
          1,
          0,
          0,
          listOf(AppointmentAttendeeSearchResult(21, "B2345CD", 2)),
          EventTierType.TIER_1,
        ),
        // Non-attended
        AppointmentAttendanceSummary(
          14,
          prisonCode,
          "Medical - Doctor",
          AppointmentLocationSummary(789, dpsLocation3.id, prisonCode, "Health Care Centre"),
          false,
          date,
          LocalTime.of(9, 30),
          LocalTime.of(9, 45),
          false,
          1,
          0,
          1,
          0,
          listOf(AppointmentAttendeeSearchResult(22, "C3456DE", 3)),
          EventTierType.TIER_1,
        ),
      ),
    )

    validateNoMessagesSent()
    verifyNoInteractions(telemetryClient)
    verifyNoInteractions(auditService)
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance-summaries.sql",
  )
  @Test
  fun `get appointment attendance summary for cancelled, deleted and removed statuses success`() {
    val prisonCode = RISLEY_PRISON_CODE
    val date = LocalDate.now().minusDays(1)

    val dpsLocation1 = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), prisonCode, localName = "Education 1")
    val dpsLocation2 = dpsLocation(UUID.fromString("44444444-4444-4444-4444-444444444444"), prisonCode, localName = "Chapel")
    val dpsLocation3 = dpsLocation(UUID.fromString("77777777-7777-7777-7777-777777777777"), prisonCode, localName = "Health Care Centre")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "RSI",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation1, dpsLocation2, dpsLocation3),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation1.id, 123),
        NomisDpsLocationMapping(dpsLocation2.id, 456),
        NomisDpsLocationMapping(dpsLocation3.id, 789),
      ),
    )

    val summaries = webTestClient.getAppointmentAttendanceSummaries(prisonCode, date)!!

    assertThat(summaries).containsAll(
      listOf(
        // Repeating group appointment, appointment id 1
        AppointmentAttendanceSummary(
          1,
          prisonCode,
          "Education",
          AppointmentLocationSummary(123, dpsLocation1.id, prisonCode, "Education 1"),
          false,
          date,
          LocalTime.of(9, 0),
          LocalTime.of(10, 30),
          true,
          3,
          0,
          0,
          3,
          listOf(
            AppointmentAttendeeSearchResult(1, "A1234BC", 1),
            AppointmentAttendeeSearchResult(2, "B2345CD", 2),
            AppointmentAttendeeSearchResult(3, "C3456DE", 3),
          ),
          EventTierType.TIER_1,
        ),
        // Single appointments, id 4, cancelled appointment
        AppointmentAttendanceSummary(
          4,
          prisonCode,
          "Jehovah's Witness One to One (Chaplaincy)",
          AppointmentLocationSummary(456, dpsLocation2.id, prisonCode, "Chapel"),
          false,
          date,
          LocalTime.of(13, 45),
          LocalTime.of(14, 15),
          true,
          1,
          0,
          0,
          1,
          listOf(
            AppointmentAttendeeSearchResult(13, "A1234BC", 1),
          ),
          EventTierType.TIER_1,
        ),
      ),
    )

    // Repeating group appointment, appointment id 3 all attendees removed or deleted
    assertThat(summaries.singleOrNull { it.id == 3L }).isNull()
    // Single appointments, ids 5-8
    // Deleted appointment
    assertThat(summaries.singleOrNull { it.id == 5L }).isNull()
    // No attendees
    assertThat(summaries.singleOrNull { it.id == 6L }).isNull()
    // No attendance marked and was removed
    assertThat(summaries.singleOrNull { it.id == 7L }).isNull()
    // No attendance marked and was deleted
    assertThat(summaries.singleOrNull { it.id == 8L }).isNull()

    validateNoMessagesSent()
    verifyNoInteractions(telemetryClient)
    verifyNoInteractions(auditService)
  }

  @Nested
  @DisplayName("Mark Multiple Attendances")
  inner class MarkMultipleAttendances {

    val prisonerA1234BC = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      bookingId = 1,
      prisonId = "RSI",
    )

    val prisonerB2345CD = prisonerA1234BC.copy(prisonerNumber = "B2345CD")
    val prisonerC3456DE = prisonerA1234BC.copy(prisonerNumber = "C3456DE")
    val prisonerXX1111X = prisonerA1234BC.copy(prisonerNumber = "XX1111X")
    val prisonerYY1111Y = prisonerA1234BC.copy(prisonerNumber = "YY1111Y")

    @AfterEach
    fun afterEach() {
      verifyNoMoreInteractions(telemetryClient)
      verifyNoInteractions(auditService)
      validateNoMessagesSent()
    }

    @Sql("classpath:test_data/seed-appointment-attendance.sql")
    @Test
    fun `Mark attendances`() {
      val requests = listOf(
        MultipleAppointmentAttendanceRequest(1, listOf("A1234BC")),
        MultipleAppointmentAttendanceRequest(2, listOf("A1234BC", "B2345CD", "C3456DE")),
        MultipleAppointmentAttendanceRequest(101, listOf("XX1111X", "YY1111Y", "XXXXXX")),
        MultipleAppointmentAttendanceRequest(999, listOf("XX1111X")),
      )

      webTestClient.updateAttendances(requests, AttendanceAction.ATTENDED)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC"),
        listOf(prisonerA1234BC),
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC", "B2345CD", "C3456DE"),
        listOf(prisonerA1234BC, prisonerB2345CD, prisonerC3456DE),
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("XX1111X", "YY1111Y"),
        listOf(prisonerXX1111X, prisonerYY1111Y),
      )

      val appointments = webTestClient.getAppointmentDetailsByIds(listOf(1, 2, 101))!!

      verifyAttendance(appointments, 1, "A1234BC")
      verifyAttendance(appointments, 2, "A1234BC")
      verifyAttendance(appointments, 2, "B2345CD")
      verifyAttendance(appointments, 2, "C3456DE")
      verifyAttendance(appointments, 2, "A1234BC")
      verifyAttendance(appointments, 101, "XX1111X")
      verifyAttendance(appointments, 101, "YY1111Y")

      verifyResetAttendance(appointments, 1, "B2345CD")

      verify(telemetryClient, times(3)).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      telemetryPropertyMap.allValues hasSize 3
      telemetryMetricsMap.allValues hasSize 3

      verifyTelemetryPropertyMap(telemetryPropertyMap.firstValue, 1)
      verifyTelemetryPropertyMap(telemetryPropertyMap.secondValue, 2)
      verifyTelemetryPropertyMap(telemetryPropertyMap.thirdValue, 101)

      verifyTelemetryMetricsMap(telemetryMetricsMap.firstValue, 1, 0, 0)
      verifyTelemetryMetricsMap(telemetryMetricsMap.secondValue, 3, 0, 1)
      verifyTelemetryMetricsMap(telemetryMetricsMap.thirdValue, 2, 0, 0)
    }

    @Sql("classpath:test_data/seed-appointment-attendance.sql")
    @Test
    fun `Mark attendances as non-attended`() {
      val requests = listOf(
        MultipleAppointmentAttendanceRequest(1, listOf("A1234BC")),
        MultipleAppointmentAttendanceRequest(2, listOf("A1234BC", "B2345CD", "C3456DE")),
        MultipleAppointmentAttendanceRequest(101, listOf("XX1111X", "YY1111Y", "XXXXXX")),
        MultipleAppointmentAttendanceRequest(999, listOf("XX1111X")),
      )

      webTestClient.updateAttendances(requests, AttendanceAction.NOT_ATTENDED)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC"),
        listOf(prisonerA1234BC),
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC", "B2345CD", "C3456DE"),
        listOf(prisonerA1234BC, prisonerB2345CD, prisonerC3456DE),
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("XX1111X", "YY1111Y"),
        listOf(prisonerXX1111X, prisonerYY1111Y),
      )

      val appointments = webTestClient.getAppointmentDetailsByIds(listOf(1, 2, 101))!!

      verifyAttendance(appointments, 1, "A1234BC", false)
      verifyAttendance(appointments, 2, "A1234BC", false)
      verifyAttendance(appointments, 2, "B2345CD", false)
      verifyAttendance(appointments, 2, "C3456DE", false)
      verifyAttendance(appointments, 2, "A1234BC", false)
      verifyAttendance(appointments, 101, "XX1111X", false)
      verifyAttendance(appointments, 101, "YY1111Y", false)

      verifyResetAttendance(appointments, 1, "B2345CD")

      verify(telemetryClient, times(3)).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      telemetryPropertyMap.allValues hasSize 3
      telemetryMetricsMap.allValues hasSize 3

      verifyTelemetryPropertyMap(telemetryPropertyMap.firstValue, 1)
      verifyTelemetryPropertyMap(telemetryPropertyMap.secondValue, 2)
      verifyTelemetryPropertyMap(telemetryPropertyMap.thirdValue, 101)

      verifyTelemetryMetricsMap(telemetryMetricsMap.firstValue, 0, 1, 0)
      verifyTelemetryMetricsMap(telemetryMetricsMap.secondValue, 0, 3, 1)
      verifyTelemetryMetricsMap(telemetryMetricsMap.thirdValue, 0, 2, 0)
    }

    @Sql("classpath:test_data/seed-appointment-attendance.sql")
    @Test
    fun `Reset attendances`() {
      val requests = listOf(
        MultipleAppointmentAttendanceRequest(1, listOf("A1234BC")),
        MultipleAppointmentAttendanceRequest(2, listOf("A1234BC", "B2345CD", "C3456DE")),
        MultipleAppointmentAttendanceRequest(101, listOf("XX1111X", "YY1111Y", "XXXXXX")),
        MultipleAppointmentAttendanceRequest(999, listOf("XX1111X")),
      )

      webTestClient.updateAttendances(requests, AttendanceAction.RESET)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC"),
        listOf(prisonerA1234BC),
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC", "B2345CD", "C3456DE"),
        listOf(prisonerA1234BC, prisonerB2345CD, prisonerC3456DE),
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("XX1111X", "YY1111Y"),
        listOf(prisonerXX1111X, prisonerYY1111Y),
      )

      val appointments = webTestClient.getAppointmentDetailsByIds(listOf(1, 2, 101))!!

      verifyResetAttendance(appointments, 1, "A1234BC")
      verifyResetAttendance(appointments, 1, "B2345CD")
      verifyResetAttendance(appointments, 2, "A1234BC")
      verifyResetAttendance(appointments, 2, "B2345CD")
      verifyResetAttendance(appointments, 2, "C3456DE")
      verifyResetAttendance(appointments, 1, "B2345CD")
      verifyResetAttendance(appointments, 101, "XX1111X")
      verifyResetAttendance(appointments, 101, "YY1111Y")

      verify(telemetryClient, times(3)).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMap.capture(),
        telemetryMetricsMap.capture(),
      )

      telemetryPropertyMap.allValues hasSize 3
      telemetryMetricsMap.allValues hasSize 3

      verifyTelemetryPropertyMap(telemetryPropertyMap.firstValue, 1)
      verifyTelemetryPropertyMap(telemetryPropertyMap.secondValue, 2)
      verifyTelemetryPropertyMap(telemetryPropertyMap.thirdValue, 101)

      verifyTelemetryMetricsMap(telemetryMetricsMap.firstValue, 0, 0, 0)
      verifyTelemetryMetricsMap(telemetryMetricsMap.secondValue, 0, 0, 2)
      verifyTelemetryMetricsMap(telemetryMetricsMap.thirdValue, 0, 0, 0)
    }
  }

  private fun verifyResetAttendance(appointments: List<AppointmentDetails>, id: Long, prisonerNumber: String) {
    with(appointments.first { it.id == id }) {
      with(attendees.first { it.prisoner.prisonerNumber == prisonerNumber }) {
        assertThat(attended).isNull()
        assertThat(attendanceRecordedTime).isNull()
        assertThat(attendanceRecordedBy).isNull()
      }
    }
  }

  private fun verifyAttendance(appointments: List<AppointmentDetails>, id: Long, prisonerNumber: String, attended: Boolean = true) {
    with(appointments.first { it.id == id }) {
      with(attendees.first { it.prisoner.prisonerNumber == prisonerNumber }) {
        assertThat(attended).isEqualTo(attended)
        assertThat(attendanceRecordedTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
        assertThat(attendanceRecordedBy).isEqualTo("test-client")
      }
    }
  }

  private fun verifyTelemetryPropertyMap(map: Map<String, String>, appointmentId: Long, prisonCode: String = "RSI") {
    assertThat(map[USER_PROPERTY_KEY]).isEqualTo("test-client")
    assertThat(map[PRISON_CODE_PROPERTY_KEY]).isEqualTo(prisonCode)
    assertThat(map[APPOINTMENT_ID_PROPERTY_KEY]).isEqualTo(appointmentId.toString())
  }

  private fun verifyTelemetryMetricsMap(map: Map<String, Double>, attended: Int, nonAttended: Int, changed: Int) {
    assertThat(map[PRISONERS_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(attended.toDouble())
    assertThat(map[PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY]).isEqualTo(nonAttended.toDouble())
    assertThat(map[PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY]).isEqualTo(changed.toDouble())
    assertThat(map[EVENT_TIME_MS_METRIC_KEY]).isGreaterThan(0.0)
  }

  private fun WebTestClient.getAppointmentAttendanceSummaries(
    prisonCode: String,
    date: LocalDate,
    additionalFilters: String = "",
  ) = get()
    .uri("/appointments/$prisonCode/attendance-summaries?date=$date$additionalFilters")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(AppointmentAttendanceSummary::class.java)
    .returnResult().responseBody

  private fun WebTestClient.updateAttendances(
    requests: List<MultipleAppointmentAttendanceRequest>,
    action: AttendanceAction?,
  ) = put()
    .uri("/appointments/updateAttendances?action=$action")
    .bodyValue(requests)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isNoContent
}
