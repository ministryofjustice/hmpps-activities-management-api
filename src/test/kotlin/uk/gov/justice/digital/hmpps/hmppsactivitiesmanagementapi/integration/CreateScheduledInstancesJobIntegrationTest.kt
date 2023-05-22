package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.DayOfWeek
import java.time.LocalDate

class CreateScheduledInstancesJobIntegrationTest : IntegrationTestBase() {

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `Schedules instances of activities sessions including bank holidays and suspended schedules`() {
    // Today is stubbed as a bank holiday via the BankHolidayExtension class, via the IntegrationTestBase class.
    jdbcTemplate.update(
      "update activity_schedule_slot set " +
        "monday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY)}, " +
        "tuesday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY)}, " +
        "wednesday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY)}, " +
        "thursday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY)}, " +
        "friday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY)}, " +
        "saturday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY)}, " +
        "sunday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY)}",
    )
    jdbcTemplate.update("update activity_schedule set runs_on_bank_holiday = true")

    webTestClient.createScheduledInstances()

    val actualNumberOfScheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '${LocalDate.now()}'",
    )

    assertThat(actualNumberOfScheduledInstances).isEqualTo(2)
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `Do not schedule instances of activity sessions on a bank holiday`() {
    // Today is stubbed as a bank holiday via the BankHolidayExtension class, via the IntegrationTestBase class.
    jdbcTemplate.update(
      "update activity_schedule_slot set " +
        "monday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.MONDAY)}, " +
        "tuesday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.TUESDAY)}, " +
        "wednesday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.WEDNESDAY)}, " +
        "thursday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.THURSDAY)}, " +
        "friday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.FRIDAY)}, " +
        "saturday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.SATURDAY)}, " +
        "sunday_flag = ${LocalDate.now().dayOfWeek.equals(DayOfWeek.SUNDAY)}",
    )
    jdbcTemplate.update("update activity_schedule set runs_on_bank_holiday = false")

    webTestClient.createScheduledInstances()

    val actualNumberOfScheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '${LocalDate.now()}'",
    )

    assertThat(actualNumberOfScheduledInstances).isEqualTo(0)
  }

  private fun WebTestClient.createScheduledInstances() {
    post()
      .uri("/job/create-scheduled-instances")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(3000)
  }
}
