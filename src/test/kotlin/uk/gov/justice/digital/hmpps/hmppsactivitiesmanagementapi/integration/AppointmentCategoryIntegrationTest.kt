package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

class AppointmentCategoryIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get list of appointment categories`() {
    prisonApiMockServer.stubGetAppointmentScheduleReasons()
    assertThat(webTestClient.getAppointmentCategories()!!).containsExactly(
      AppointmentCategorySummary(code = "AC1", description = "Appointment Category 1"),
      AppointmentCategorySummary(code = "AC2", description = "Appointment Category 2"),
      AppointmentCategorySummary(code = "AC3", description = "Appointment Category 3"),
    )
  }

  @Test
  fun `authorisation required`() {
    webTestClient.get()
      .uri("/appointment-categories")
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun WebTestClient.getAppointmentCategories() =
    get()
      .uri("/appointment-categories")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AppointmentCategorySummary::class.java)
      .returnResult().responseBody
}
