package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.DayOfWeek
import java.time.LocalDate

class CreateActivitySessionsJobIntegrationTest : IntegrationTestBase() {

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `Schedule instances of activity sessions`() {
    val sixtyDaysFromToday = LocalDate.now().plusDays(60)

    jdbcTemplate.update(
      "update activity_schedule set " +
        "monday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.MONDAY)}, " +
        "tuesday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.TUESDAY)}, " +
        "wednesday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.WEDNESDAY)}, " +
        "thursday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.THURSDAY)}, " +
        "friday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.FRIDAY)}, " +
        "saturday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.SATURDAY)}, " +
        "sunday_flag = ${sixtyDaysFromToday.dayOfWeek.equals(DayOfWeek.SUNDAY)}"
    )
    webTestClient.scheduleInstances()

    val actualNumberOfScheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$sixtyDaysFromToday'"
    )

    assertThat(actualNumberOfScheduledInstances).isEqualTo(2)
  }

  private fun WebTestClient.scheduleInstances() {
    post()
      .uri("/job/create-activity-sessions")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}
