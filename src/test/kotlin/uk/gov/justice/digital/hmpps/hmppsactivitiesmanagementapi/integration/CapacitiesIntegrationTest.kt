package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated

class CapacitiesIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `get capacity of a category`() {
    with(webTestClient.getCategoryCapacity("PVI", 1)!!) {
      assertThat(capacity).isEqualTo(20)
      assertThat(allocated).isEqualTo(4)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `get capacity of an activity`() {
    with(webTestClient.getActivityCapacity(1)!!) {
      assertThat(capacity).isEqualTo(20)
      assertThat(allocated).isEqualTo(4)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `get capacity of a schedule`() {
    with(webTestClient.getScheduleCapacity(1)!!) {
      assertThat(capacity).isEqualTo(10)
      assertThat(allocated).isEqualTo(2)
    }
  }

  private fun WebTestClient.getCategoryCapacity(prisonCode: String, categoryId: Long) =
    get()
      .uri("/prison/$prisonCode/activity-categories/$categoryId/capacity")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CapacityAndAllocated::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getActivityCapacity(activityId: Long) =
    get()
      .uri("/activities/$activityId/capacity")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CapacityAndAllocated::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getScheduleCapacity(scheduleId: Long) =
    get()
      .uri("/schedules/$scheduleId/capacity")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CapacityAndAllocated::class.java)
      .returnResult().responseBody
}
