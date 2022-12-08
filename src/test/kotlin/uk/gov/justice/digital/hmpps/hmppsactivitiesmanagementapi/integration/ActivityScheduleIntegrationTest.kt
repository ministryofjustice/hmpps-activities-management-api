package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation

class ActivityScheduleIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get only active allocations for Maths`() {
    webTestClient.getAllocationsBy(1)!!
      .also { assertThat(it).hasSize(2) }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all active allocations for Maths`() {
    webTestClient.getAllocationsBy(1, false)!!
      .also { assertThat(it).hasSize(3) }
  }

  private fun WebTestClient.getAllocationsBy(scheduleId: Long, activeOnly: Boolean? = null) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId/allocations")
          .maybeQueryParam("activeOnly", activeOnly)
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Allocation::class.java)
      .returnResult().responseBody

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get schedules by their ids`() {
    with(webTestClient.getScheduleBy(1)!!) {
      assertThat(id).isEqualTo(1)
    }

    with(webTestClient.getScheduleBy(2)!!) {
      assertThat(id).isEqualTo(2)
    }
  }

  private fun WebTestClient.getScheduleBy(scheduleId: Long) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId")
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySchedule::class.java)
      .returnResult().responseBody
}
