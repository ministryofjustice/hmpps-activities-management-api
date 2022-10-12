package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison

class RolloutPrisonIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get active rollout prison HMP Pentonville`() {
    with(webTestClient.getPrisonByCode("PVI")!!) {
      assertThat(id).isEqualTo(1)
      assertThat(code).isEqualTo("PVI")
      assertThat(description).isEqualTo("HMP Pentonville")
      assertThat(active).isTrue
    }
  }

  @Test
  fun `get inactive rollout prison HMP Moorland`() {
    with(webTestClient.getPrisonByCode("MDI")!!) {
      assertThat(id).isEqualTo(2)
      assertThat(code).isEqualTo("MDI")
      assertThat(description).isEqualTo("HMP Moorland")
      assertThat(active).isFalse
    }
  }

  private fun WebTestClient.getPrisonByCode(code: String) =
    get()
      .uri("/rollout-prisons/$code")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RolloutPrison::class.java)
      .returnResult().responseBody
}
