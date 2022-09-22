package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier
import java.time.LocalDate

class ActivityIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get maths activity with morning and afternoon sessions`() {
    val result = webTestClient.getActivityById(1)!!

    with(result) {
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(category).isEqualTo(ActivityCategory(1, "C1", "Category 1"))
      assertThat(tier).isEqualTo(ActivityTier(1, "T1          ", "Tier 1"))
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(sessions).hasSize(2)
    }

    with(result.sessions.select(1)) {
      assertThat(description).isEqualTo("Maths AM")
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      assertThat(prisoners.select(1).prisonerNumber).isEqualTo("A11111A")
      assertThat(prisoners.select(2).prisonerNumber).isEqualTo("A22222A")
    }

    with(result.sessions.select(2)) {
      assertThat(description).isEqualTo("Maths PM")
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      assertThat(prisoners.select(3).prisonerNumber).isEqualTo("A11111A")
      assertThat(prisoners.select(4).prisonerNumber).isEqualTo("A22222A")
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-2.sql"
  )
  @Test
  fun `get english activity with morning and afternoon sessions`() {
    val result = webTestClient.getActivityById(2)!!

    with(result) {
      assertThat(summary).isEqualTo("English")
      assertThat(description).isEqualTo("English Level 2")
      assertThat(category).isEqualTo(ActivityCategory(2, "C2", "Category 2"))
      assertThat(tier).isEqualTo(ActivityTier(2, "T2          ", "Tier 2"))
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(active).isTrue
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(sessions).hasSize(2)
    }

    with(result.sessions.select(3)) {
      assertThat(description).isEqualTo("English AM")
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      assertThat(prisoners.select(5).prisonerNumber).isEqualTo("B11111B")
      assertThat(prisoners.select(6).prisonerNumber).isEqualTo("B22222B")
    }

    with(result.sessions.select(4)) {
      assertThat(description).isEqualTo("English PM")
      assertThat(capacity).isEqualTo(10)
      assertThat(daysOfWeek).isEqualTo("1000000")
      assertThat(prisoners).hasSize(2)
      assertThat(prisoners.select(7).prisonerNumber).isEqualTo("B11111B")
      assertThat(prisoners.select(8).prisonerNumber).isEqualTo("B22222B")
    }
  }

  private fun WebTestClient.getActivityById(id: Long) =
    get()
      .uri("/activities/$id")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody

  private fun List<ActivitySession>.select(id: Long) = first { it.id == id }

  private fun List<ActivityPrisoner>.select(id: Long) = first { it.id == id }
}
