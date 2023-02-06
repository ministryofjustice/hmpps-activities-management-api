package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory

class AppointmentCategoryIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get list of activity categories`() {
    assertThat(webTestClient.getAppointmentCategories()!!).containsExactly(
      AppointmentCategory(id = 5, code = "LAC1", description = "Legacy Appointment Category 1", active = false, displayOrder = 1),
      AppointmentCategory(id = 2, code = "LAC2", description = "Legacy Appointment Category 2", active = false, displayOrder = 2),
      AppointmentCategory(id = 3, code = "AC1", description = "Appointment Category 1", active = true, displayOrder = 3),
      AppointmentCategory(id = 1, code = "AC2", description = "Appointment Category 2", active = true, displayOrder = null),
      AppointmentCategory(id = 4, code = "AC3", description = "Appointment Category 3", active = true, displayOrder = null)
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
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AppointmentCategory::class.java)
      .returnResult().responseBody
}
