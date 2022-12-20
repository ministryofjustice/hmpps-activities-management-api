package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory

class ActivityCategoryIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get list of activity categories`() {
    with(webTestClient.getActivityCategories()!!) {
      assertThat(size).isEqualTo(3)
      assertThat(get(0)).isEqualTo(ActivityCategory(id = 1, code = "C1", name = "Category 1", description = "Description of Category 1"))
      assertThat(get(1)).isEqualTo(ActivityCategory(id = 2, code = "C2", name = "Category 2", description = "Description of Category 2"))
      assertThat(get(2)).isEqualTo(ActivityCategory(id = 3, code = "C3", name = "Category 3", description = "Description of Category 3"))
    }
  }

  private fun WebTestClient.getActivityCategories() =
    get()
      .uri("/activity-categories")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityCategory::class.java)
      .returnResult().responseBody
}
