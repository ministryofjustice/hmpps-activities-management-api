package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentOccurrenceDetailsIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get appointment occurrence details authorisation required`() {
    webTestClient.get()
      .uri("/appointment-occurrence-details/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment occurrence details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-occurrence-details/-1")
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
      .uri("/appointment-occurrence-details/3")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
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

    val appointmentOccurrenceDetails = webTestClient.getAppointmentOccurrenceDetailsById(2)!!

    assertThat(appointmentOccurrenceDetails).isEqualTo(
      AppointmentDetails(
        2,
        1,
        null,
        AppointmentType.INDIVIDUAL,
        1,
        "TPR",
        "Appointment description (Appointment Category 1)",
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "Tim", "Harrison", "TPR", "1-2-3"),
        ),
        AppointmentCategorySummary("AC1", "Appointment Category 1"),
        "Appointment description",
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        "Appointment level comment",
        null,
        false,
        false,
        false,
        appointmentOccurrenceDetails.createdTime,
        UserSummary(1, "TEST.USER", "TEST1", "USER1"),
        null,
        null,
        null,
        null,
      ),
    )

    assertThat(appointmentOccurrenceDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Sql(
    "classpath:test_data/seed-appointment-set-id-6.sql",
  )
  @Test
  fun `get occurrence details from a set of appointments created in bulk`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC"),
      listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR")),
    )

    val appointmentOccurrenceDetails = webTestClient.getAppointmentOccurrenceDetailsById(6)!!

    assertThat(appointmentOccurrenceDetails).isEqualTo(
      AppointmentDetails(
        6,
        6,
        AppointmentSetSummary(6, 3),
        AppointmentType.INDIVIDUAL,
        1,
        "TPR",
        "Appointment description (Appointment Category 1)",
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "Tim", "Harrison", "TPR", "1-2-3"),
        ),
        AppointmentCategorySummary("AC1", "Appointment Category 1"),
        "Appointment description",
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(9, 15),
        "Medical appointment for A1234BC",
        null,
        false,
        false,
        false,
        appointmentOccurrenceDetails.createdTime,
        UserSummary(1, "TEST.USER", "TEST1", "USER1"),
        null,
        null,
        null,
        null,
      ),
    )

    assertThat(appointmentOccurrenceDetails.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  private fun WebTestClient.getAppointmentOccurrenceDetailsById(id: Long) =
    get()
      .uri("/appointment-occurrence-details/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentDetails::class.java)
      .returnResult().responseBody
}
