package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory

class AppointmentCategoryIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get list of activity categories`() {
    with(webTestClient.getAppointmentCategories()!!) {
      assertThat(size).isEqualTo(5)
      assertThat(get(0)).isEqualTo(AppointmentCategory(id = 5, code = "LAC1", description = "Legacy Appointment Category 1", active = false, displayOrder = 1))
      assertThat(get(1)).isEqualTo(AppointmentCategory(id = 2, code = "LAC2", description = "Legacy Appointment Category 2", active = false, displayOrder = 2))
      assertThat(get(2)).isEqualTo(AppointmentCategory(id = 3, code = "AC1", description = "Appointment Category 1", active = true, displayOrder = 3))
      assertThat(get(3)).isEqualTo(AppointmentCategory(id = 1, code = "AC2", description = "Appointment Category 2", active = true, displayOrder = null))
      assertThat(get(4)).isEqualTo(AppointmentCategory(id = 4, code = "AC3", description = "Appointment Category 3", active = true, displayOrder = null))
    }
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