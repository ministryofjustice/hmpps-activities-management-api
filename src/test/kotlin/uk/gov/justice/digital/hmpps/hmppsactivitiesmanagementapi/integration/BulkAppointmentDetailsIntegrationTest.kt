package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class BulkAppointmentDetailsIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get bulk appointment details authorisation required`() {
    webTestClient.get()
      .uri("/bulk-appointment-details/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get bulk appointment details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/bulk-appointment-details/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-set-id-6.sql",
  )
  @Test
  fun `get bulk appointment details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC", "B2345CD", "C3456DE"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST01",
          lastName = "PRISONER01",
          prisonId = "TPR",
          cellLocation = "1-2-3",
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "B2345CD",
          bookingId = 457,
          firstName = "TEST02",
          lastName = "PRISONER02",
          prisonId = "TPR",
          cellLocation = "1-2-4",
        ),
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "C3456DE",
          bookingId = 458,
          firstName = "TEST03",
          lastName = "PRISONER03",
          prisonId = "TPR",
          cellLocation = "1-2-5",
        ),
      ),
    )

    val details = webTestClient.getBulkAppointmentDetailsById(6)!!

    val category = AppointmentCategorySummary("AC1", "Appointment Category 1")
    val appointmentDescription = "Appointment description"
    val createdBy = UserSummary(1, "TEST.USER", "TEST1", "USER1")
    assertThat(details).isEqualTo(
      BulkAppointmentDetails(
        6,
        "TPR",
        if (!appointmentDescription.isNullOrEmpty()) "$appointmentDescription (${category.description})" else category.description,
        category,
        appointmentDescription,
        AppointmentLocationSummary(123, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        occurrences = listOf(
          appointmentOccurrenceDetails(
            6, 6, BulkAppointmentSummary(6, 3), 1,
            listOf(
              PrisonerSummary("A1234BC", 456, "TEST01", "PRISONER01", "TPR", "1-2-3"),
            ),
            category, appointmentDescription,
            LocalTime.of(9, 0),
            LocalTime.of(9, 15),
            "Medical appointment for A1234BC",
            details.created, createdBy, null, null,
          ),
          appointmentOccurrenceDetails(
            7, 7, BulkAppointmentSummary(6, 3), 1,
            listOf(
              PrisonerSummary("B2345CD", 457, "TEST02", "PRISONER02", "TPR", "1-2-4"),
            ),
            category, appointmentDescription,
            LocalTime.of(9, 15),
            LocalTime.of(9, 30),
            "Medical appointment for B2345CD",
            details.created, createdBy, null, null,
          ),
          appointmentOccurrenceDetails(
            8, 8, BulkAppointmentSummary(6, 3), 1,
            listOf(
              PrisonerSummary("C3456DE", 458, "TEST03", "PRISONER03", "TPR", "1-2-5"),
            ),
            category, appointmentDescription,
            LocalTime.of(9, 30),
            LocalTime.of(9, 45),
            "Medical appointment for C3456DE",
            details.created, createdBy, null, null,
          ),
        ),
        details.created,
        createdBy,
      ),
    )

    assertThat(details.created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  private fun WebTestClient.getBulkAppointmentDetailsById(id: Long) =
    get()
      .uri("/bulk-appointment-details/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(BulkAppointmentDetails::class.java)
      .returnResult().responseBody
}
