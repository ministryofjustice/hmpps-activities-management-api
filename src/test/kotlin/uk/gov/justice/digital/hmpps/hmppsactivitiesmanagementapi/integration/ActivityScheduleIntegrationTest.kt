package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import java.time.LocalDate
import java.time.LocalTime

class ActivityScheduleIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all schedules for Pentonville prison`() {
    val schedules =
      webTestClient.getSchedulesByPrison("PVI", LocalDate.of(2022, 10, 10))!!
        .also { assertThat(it).hasSize(1) }

    val schedule = with(schedules.first()) {
      assertThat(allocations).hasSize(2)
      assertThat(instances).hasSize(1)
      this
    }

    schedule.prisoner("A11111A")
    schedule.prisoner("A22222A")

    with(schedule.instances.first()) {
      assertThat(date).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(cancelled).isFalse
      assertThat(startTime).isEqualTo(LocalTime.of(10, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(11, 0))
    }
  }

  private fun WebTestClient.getSchedulesByPrison(prisonCode: String, date: LocalDate? = null) =
    get()
      .uri("/schedules/$prisonCode?date=${date ?: LocalDate.now()}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivitySchedule::class.java)
      .returnResult().responseBody
}
