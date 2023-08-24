package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import java.time.LocalDate

class RolloutIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get rollout prison HMP Pentonville - active activities, inactive appointments`() {
    with(webTestClient.getPrisonByCode("PVI")!!) {
      assertThat(activitiesRolledOut).isTrue
      assertThat(activitiesRolloutDate).isEqualTo(LocalDate.of(2022, 12, 22))
      assertThat(appointmentsRolledOut).isFalse
      assertThat(appointmentsRolloutDate).isNull()
    }
  }

  @Test
  fun `get inactive rollout prison HMP Moorland - both active activities and appointments`() {
    with(webTestClient.getPrisonByCode("MDI")!!) {
      assertThat(activitiesRolledOut).isTrue
      assertThat(activitiesRolloutDate).isEqualTo(LocalDate.of(2022, 12, 22))
      assertThat(appointmentsRolledOut).isTrue
      assertThat(appointmentsRolloutDate).isEqualTo(LocalDate.of(2022, 12, 23))
    }
  }

  private fun WebTestClient.getPrisonByCode(code: String) =
    get()
      .uri("/rollout/$code")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RolloutPrisonPlan::class.java)
      .returnResult().responseBody
}
