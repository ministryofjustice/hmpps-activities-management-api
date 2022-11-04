package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated

class CapacitiesIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  fun `get scheduled maths activities with morning and afternoon`() {
    with(webTestClient.getCategoryCapacity("PVI", 1)!!) {
      assertThat(capacity).isEqualTo(20)
      assertThat(allocated).isEqualTo(4)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  fun `404 when category id not found`() {
    with(webTestClient.getCategoryCapacity404("MDI", 5)!!) {
      assertThat(developerMessage).isEqualTo("Activity category 5 not found")
    }
  }

  private fun WebTestClient.getCategoryCapacity(prisonCode: String, categoryId: Long) =
    getCategoryCapacityUri(prisonCode, categoryId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CapacityAndAllocated::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getCategoryCapacity404(prisonCode: String, categoryId: Long) =
    getCategoryCapacityUri(prisonCode, categoryId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getCategoryCapacityUri(prisonCode: String, categoryId: Long) =
    get()
      .uri("/prison/$prisonCode/activity-categories/$categoryId/capacity")
}
