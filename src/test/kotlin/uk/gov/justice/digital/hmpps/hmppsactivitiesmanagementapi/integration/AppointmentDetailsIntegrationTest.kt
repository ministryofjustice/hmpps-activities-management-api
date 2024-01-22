package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendeeSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelEventTier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentDetailsIntegrationTest : IntegrationTestBase() {
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
    "classpath:test_data/seed-appointment-deleted-id-2.sql",
  )
  @Test
  fun `get deleted appointment details returns 404 not found`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC"),
      listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR")),
    )

    webTestClient.get()
      .uri("/appointments/3/details")
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

    prisonApiMockServer.stubGetUserDetailsList(
      listOf("TEST.USER", "PREV.ATTENDANCE.RECORDED.BY"),
      listOf(
        userDetail(1, "TEST.USER", "TEST", "USER"),
        userDetail(2, "PREV.ATTENDANCE.RECORDED.BY", "ATTENDANCE", "USER"),
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

    val userSummary = UserSummary(2, "PREV.ATTENDANCE.RECORDED.BY", "ATTENDANCE", "USER")
    with(attendeesMap[4]!!) {
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
    }
    with(attendeesMap[5]!!) {
      attended!! isBool true
      attendanceRecordedTime!!.toLocalDate() isEqualTo LocalDate.now().minusDays(1)
      attendanceRecordedBy isEqualTo userSummary
    }
    with(attendeesMap[6]!!) {
      attended!! isBool false
      attendanceRecordedTime!!.toLocalDate() isEqualTo LocalDate.now().minusDays(1)
      attendanceRecordedBy isEqualTo userSummary
    }
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC"),
      listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR")),
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
            PrisonerSummary("A1234BC", 456, "Tim", "Harrison", "ACTIVE IN", "TPR", "1-2-3"),
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
        UserSummary(1, "TEST.USER", "TEST1", "USER1"),
        false,
        null,
        null,
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
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))
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
            PrisonerSummary("A1234BC", 456, "Tim", "Harrison", "ACTIVE IN", "TPR", "1-2-3"),
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
        UserSummary(1, "TEST.USER", "TEST1", "USER1"),
        false,
        null,
        null,
        false,
        null,
        null,
      ),
    )

    assertThat(appointmentDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  private fun WebTestClient.getAppointmentDetailsById(id: Long) =
    get()
      .uri("/appointments/$id/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentDetails::class.java)
      .returnResult().responseBody
}
