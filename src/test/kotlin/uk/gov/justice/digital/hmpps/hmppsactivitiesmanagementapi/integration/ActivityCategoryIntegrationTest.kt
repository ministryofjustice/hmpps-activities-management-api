package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.gymSportsFitnessCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.inductionCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.industriesCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.interventionsCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.leisureAndSocialCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.servicesCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory

class ActivityCategoryIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get list of activity categories`() {
    assertThat(webTestClient.getActivityCategories()!!).containsExactlyInAnyOrder(
      educationCategory,
      industriesCategory,
      servicesCategory,
      gymSportsFitnessCategory,
      inductionCategory,
      interventionsCategory,
      leisureAndSocialCategory
    )
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
