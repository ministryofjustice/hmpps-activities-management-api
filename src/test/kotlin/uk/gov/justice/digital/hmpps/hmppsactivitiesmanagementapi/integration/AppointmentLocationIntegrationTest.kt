package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary

class AppointmentLocationIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get list of appointment locations`() {
    prisonApiMockServer.stubGetLocationsForAppointments(moorlandPrisonCode, 1)
    assertThat(webTestClient.getAppointmentLocations()!!).containsExactly(
      AppointmentLocationSummary(id = 1, prisonCode = moorlandPrisonCode, description = "Test Appointment Location"),
    )
  }

  @Test
  fun `authorisation required`() {
    webTestClient.get()
      .uri("/appointment-locations/{prisonCode}", moorlandPrisonCode)
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun WebTestClient.getAppointmentLocations() =
    get()
      .uri("/appointment-locations/{prisonCode}", moorlandPrisonCode)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AppointmentLocationSummary::class.java)
      .returnResult().responseBody
}
