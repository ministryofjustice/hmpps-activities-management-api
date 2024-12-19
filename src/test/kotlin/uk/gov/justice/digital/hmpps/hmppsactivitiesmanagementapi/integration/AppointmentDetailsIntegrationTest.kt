package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendeeSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentDetailsIntegrationTest : AppointmentsIntegrationTestBase() {
  @Test
  fun `get appointment details authorisation required`() {
    webTestClient.get()
      .uri("/appointments/1/details")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments/-1/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-attendance.sql",
  )
  @Test
  fun `get group appointment with marked attendance details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
      ),
    )

    prisonApiMockServer.stubGetLocationsForAppointments(
      RISLEY_PRISON_CODE,
      listOf(
        appointmentLocation(123, RISLEY_PRISON_CODE, userDescription = "Education 1"),
      ),
    )
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC", "B2345CD", "C3456DE"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", firstName = "A", lastName = "A", bookingId = 1, prisonId = RISLEY_PRISON_CODE),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", firstName = "B", lastName = "B", bookingId = 2, prisonId = RISLEY_PRISON_CODE),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", firstName = "C", lastName = "C", bookingId = 3, prisonId = RISLEY_PRISON_CODE),
      ),
    )

    val appointmentDetails = webTestClient.getAppointmentDetailsById(2)!!
    val attendeesMap = appointmentDetails.attendees.associateBy { it.id }

    with(attendeesMap[4]!!) {
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
    }
    with(attendeesMap[5]!!) {
      attended!! isBool true
      attendanceRecordedTime!!.toLocalDate() isEqualTo LocalDate.now().minusDays(1)
      attendanceRecordedBy isEqualTo "PREV.ATTENDANCE.RECORDED.BY"
    }
    with(attendeesMap[6]!!) {
      attended!! isBool false
      attendanceRecordedTime!!.toLocalDate() isEqualTo LocalDate.now().minusDays(1)
      attendanceRecordedBy isEqualTo "PREV.ATTENDANCE.RECORDED.BY"
    }
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC"),
      listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR", category = "H")),
    )

    val appointmentDetails = webTestClient.getAppointmentDetailsById(2)!!

    assertThat(appointmentDetails).isEqualTo(
      AppointmentDetails(
        2,
        AppointmentSeriesSummary(1, null, 1, 1),
        null,
        AppointmentType.INDIVIDUAL,
        1,
        "TPR",
        "Appointment description (Appointment Category 1)",
        attendees = listOf(
          AppointmentAttendeeSummary(
            3,
            PrisonerSummary("A1234BC", 456, "Tim", "Harrison", "ACTIVE IN", "TPR", "1-2-3", "H"),
            null,
            null,
            null,
          ),
        ),
        AppointmentCategorySummary("AC1", "Appointment Category 1"),
        eventTier().toModelEventTier(),
        eventOrganiser().toModelEventOrganiser(),
        "Appointment description",
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        false,
        "Appointment level comment",
        appointmentDetails.createdTime,
        "TEST.USER",
        false,
        null,
        null,
        false,
        false,
        null,
        null,
      ),
    )

    assertThat(appointmentDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Sql(
    "classpath:test_data/seed-appointment-set-id-6.sql",
  )
  @Test
  fun `get appointment details from an appointment set`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC"),
      listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR")),
    )

    val appointmentDetails = webTestClient.getAppointmentDetailsById(6)!!

    assertThat(appointmentDetails).isEqualTo(
      AppointmentDetails(
        6,
        null,
        AppointmentSetSummary(6, 3, 3),
        AppointmentType.INDIVIDUAL,
        1,
        "TPR",
        "Appointment description (Appointment Category 1)",
        attendees = listOf(
          AppointmentAttendeeSummary(
            6,
            PrisonerSummary("A1234BC", 456, "Tim", "Harrison", "ACTIVE IN", "TPR", "1-2-3", "P"),
            null,
            null,
            null,
          ),
        ),
        AppointmentCategorySummary("AC1", "Appointment Category 1"),
        eventTier().toModelEventTier(),
        eventOrganiser().toModelEventOrganiser(),
        "Appointment description",
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(9, 15),
        false,
        "Medical appointment for A1234BC",
        appointmentDetails.createdTime,
        "TEST.USER",
        false,
        null,
        null,
        false,
        false,
        null,
        null,
      ),
    )

    assertThat(appointmentDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `get multiple appointments by ids authorisation required`() {
    webTestClient.post()
      .uri("/appointments/details")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql("classpath:test_data/seed-appointment-attendance.sql")
  @Test
  fun `get multiple appointments by ids should return 400 if appointments are from different prisons`() {
    webTestClient.post()
      .uri("/appointments/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .bodyValue(listOf(2, 5))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Exception: Only one prison code is supported")
      .jsonPath("$.developerMessage").isEqualTo("Only one prison code is supported")
  }

  @Sql("classpath:test_data/seed-appointment-attendance.sql")
  @Test
  fun `get multiple appointments by ids succeeds`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
        appointmentCategoryReferenceCode("CANT", "Canteen"),
      ),
    )

    prisonApiMockServer.stubGetLocationsForAppointments(
      RISLEY_PRISON_CODE,
      listOf(
        appointmentLocation(123, RISLEY_PRISON_CODE, userDescription = "Education 1"),
        appointmentLocation(456, RISLEY_PRISON_CODE, userDescription = "Canteen A"),
      ),
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC", "B2345CD", "C3456DE", "Z3333ZZ"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", firstName = "A", lastName = "A", bookingId = 1, prisonId = RISLEY_PRISON_CODE),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "B2345CD", firstName = "B", lastName = "B", bookingId = 2, prisonId = RISLEY_PRISON_CODE),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "C3456DE", firstName = "C", lastName = "C", bookingId = 3, prisonId = RISLEY_PRISON_CODE),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "Z3333ZZ", firstName = "Z", lastName = "Z", bookingId = 6, prisonId = RISLEY_PRISON_CODE),
      ),
    )

    val appointments = webTestClient.getAppointmentDetailsByIds(listOf(2, 4))

    assertThat(appointments[0]).isEqualTo(
      AppointmentDetails(
        2,
        AppointmentSeriesSummary(1, AppointmentSeriesSchedule(frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3), 3, 1),
        null,
        AppointmentType.GROUP,
        2,
        "RSI",
        "Education",
        attendees = listOf(
          AppointmentAttendeeSummary(
            4,
            PrisonerSummary("A1234BC", 1, "A", "A", "ACTIVE IN", "RSI", "1-2-3", "P"),
            null,
            null,
            null,
          ),
          AppointmentAttendeeSummary(
            5,
            PrisonerSummary("B2345CD", 2, "B", "B", "ACTIVE IN", "RSI", "1-2-3", "P"),
            true,
            appointments[0].createdTime.plusDays(1),
            "PREV.ATTENDANCE.RECORDED.BY",
          ),
          AppointmentAttendeeSummary(
            6,
            PrisonerSummary("C3456DE", 3, "C", "C", "ACTIVE IN", "RSI", "1-2-3", "P"),
            false,
            appointments[0].createdTime.plusDays(1),
            "PREV.ATTENDANCE.RECORDED.BY",
          ),
        ),
        AppointmentCategorySummary("EDUC", "Education"),
        EventTier(1, "TIER_1", "Tier 1"),
        null,
        null,
        AppointmentLocationSummary(123, "RSI", "Education 1"),
        false,
        LocalDate.now(),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        false,
        null,
        appointments[0].createdTime,
        "TEST.USER",
        false,
        null,
        null,
        false,
        false,
        null,
        null,
      ),
    )

    assertThat(appointments[1]).isEqualTo(
      AppointmentDetails(
        4,
        AppointmentSeriesSummary(2, AppointmentSeriesSchedule(frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3), 1, 0),
        null,
        AppointmentType.GROUP,
        1,
        "RSI",
        "Canteen",
        attendees = listOf(
          AppointmentAttendeeSummary(
            12,
            PrisonerSummary("Z3333ZZ", 6, "Z", "Z", "ACTIVE IN", "RSI", "1-2-3", "P"),
            null,
            null,
            null,
          ),
        ),
        AppointmentCategorySummary("CANT", "Canteen"),
        EventTier(1, "TIER_1", "Tier 1"),
        null,
        null,
        AppointmentLocationSummary(456, "RSI", "Canteen A"),
        false,
        LocalDate.now(),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        false,
        null,
        appointments[1].createdTime,
        "TEST.USER",
        false,
        null,
        null,
        false,
        false,
        null,
        null,
      ),
    )

    val appointment1Attendees = appointments[0].attendees.associateBy { it.id }
    val appointment2Attendees = appointments[1].attendees.associateBy { it.id }

    with(appointment1Attendees[4]!!) {
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
    }
    with(appointment1Attendees[5]!!) {
      attended!! isBool true
      attendanceRecordedTime!!.toLocalDate() isEqualTo LocalDate.now().minusDays(1)
      attendanceRecordedBy isEqualTo "PREV.ATTENDANCE.RECORDED.BY"
    }
    with(appointment1Attendees[6]!!) {
      attended!! isBool false
      attendanceRecordedTime!!.toLocalDate() isEqualTo LocalDate.now().minusDays(1)
      attendanceRecordedBy isEqualTo "PREV.ATTENDANCE.RECORDED.BY"
    }

    appointment2Attendees.values.forEach {
      with(it) {
        attended isEqualTo null
        attendanceRecordedTime isEqualTo null
        attendanceRecordedBy isEqualTo null
      }
    }
  }
}
