package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ExternalActivitiesPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN

class ExternalActivitiesPrisonIntegrationTest : IntegrationTestBase() {

  @Test
  fun `should return list of prisons enabled for external activities sorted by prison code`() {
    webTestClient.getExternalActivitiesPrisons()
      .run {
        assertThat(this).apply {
          contains(
            ExternalActivitiesPrison("FDI", "Ford"),
            ExternalActivitiesPrison("GNI", "Grendon"),
            ExternalActivitiesPrison("HDI", "Hatfield"),
          )
          isSortedAccordingTo(compareBy { it.prisonCode })
        }
      }
  }

  @Test
  fun `should return 401 when the user is not authorized`() {
    webTestClient.get()
      .uri("/external-activities/prisons")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return 403 when the user role is invalid`() {
    webTestClient.get()
      .uri("/external-activities/prisons")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient(roles = listOf("INVALID_ROLE")))
      .exchange()
      .expectStatus().isForbidden
  }

  private fun WebTestClient.getExternalActivitiesPrisons() = get()
    .uri("/external-activities/prisons")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ExternalActivitiesPrison::class.java)
    .returnResult().responseBody
}
