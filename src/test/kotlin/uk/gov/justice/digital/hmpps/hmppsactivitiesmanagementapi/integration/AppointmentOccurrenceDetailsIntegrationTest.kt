package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDateTime
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
      .headers(setAuthorisation(roles = listOf()))
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
      .headers(setAuthorisation(roles = listOf()))
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
      appointmentOccurrenceDetails(
        2,
        1,
        sequenceNumber = 1,
        appointmentDescription = "Appointment description",
        created = appointmentOccurrenceDetails.created,
        updated = null,
      ),
    )

    assertThat(appointmentOccurrenceDetails.created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Sql(
    "classpath:test_data/seed-bulk-appointment-id-6.sql",
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
      appointmentOccurrenceDetails(
        6,
        6,
        sequenceNumber = 1,
        appointmentDescription = "Appointment description",
        created = appointmentOccurrenceDetails.created,
        updated = null,
      ),
    )

    assertThat(appointmentOccurrenceDetails.created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  private fun WebTestClient.getAppointmentOccurrenceDetailsById(id: Long) =
    get()
      .uri("/appointment-occurrence-details/$id")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentOccurrenceDetails::class.java)
      .returnResult().responseBody
}
