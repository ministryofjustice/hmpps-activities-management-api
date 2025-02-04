package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN

class AppointmentLocationIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get list of appointment locations`() {
    prisonApiMockServer.stubGetLocationsForAppointments(MOORLAND_PRISON_CODE, 1)
    assertThat(webTestClient.getAppointmentLocations()!!).containsExactly(
      AppointmentLocationSummary(id = 1, prisonCode = MOORLAND_PRISON_CODE, description = "Test Appointment Location User Description"),
    )
  }

  @Test
  fun `authorisation required`() {
    webTestClient.get()
      .uri("/appointment-locations/{prisonCode}", MOORLAND_PRISON_CODE)
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun WebTestClient.getAppointmentLocations() = get()
    .uri("/appointment-locations/{prisonCode}", MOORLAND_PRISON_CODE)
    .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(AppointmentLocationSummary::class.java)
    .returnResult().responseBody
}
