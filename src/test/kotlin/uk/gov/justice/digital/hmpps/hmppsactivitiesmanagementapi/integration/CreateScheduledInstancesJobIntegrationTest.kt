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
  val now: LocalDate = LocalDate.now()

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `creates instances for activity sessions on bank holidays and for suspended schedules`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(true)

    webTestClient.createScheduledInstances()

    val scheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$now'",
    )

    assertThat(scheduledInstances).isEqualTo(2)
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `Do not create instances for activity sessions when not marked as running on a bank holiday`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(false)

    webTestClient.createScheduledInstances()

    val scheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$now'",
    )

    assertThat(scheduledInstances).isEqualTo(0)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-with-old-instances.sql")
  fun `old instances are filtered and leaves the past intact when creating new sessions`() {
    val yesterday: LocalDate = LocalDate.now().minusDays(1)
    val dayBeforeYesterday: LocalDate = LocalDate.now().minusDays(2)
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(true)

    // Update the two existing instances to be in the past and the job should leave these unchanged.
    jdbcTemplate.update("update scheduled_instance set session_date = '$dayBeforeYesterday' where session_date = '2022-10-10'")
    jdbcTemplate.update("update scheduled_instance set session_date = '$yesterday' where session_date = '2022-10-11'")

    webTestClient.createScheduledInstances()

    // Should create 1 instance for today (schedule_ahead_days defaults to 0)
    val createdForToday = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$now'",
    )
    assertThat(createdForToday).isEqualTo(1)

    // Table should contain 3 instances in total
    val allInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where activity_schedule_id = 1",
    )
    assertThat(allInstances).isEqualTo(3)
  }

  private fun WebTestClient.createScheduledInstances() {
    post()
      .uri("/job/create-scheduled-instances")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(3000)
  }

  private fun updateSlotsToRunToday() {
    jdbcTemplate.update(
      "update activity_schedule_slot set " +
        "monday_flag = ${now.dayOfWeek.equals(DayOfWeek.MONDAY)}, " +
        "tuesday_flag = ${now.dayOfWeek.equals(DayOfWeek.TUESDAY)}, " +
        "wednesday_flag = ${now.dayOfWeek.equals(DayOfWeek.WEDNESDAY)}, " +
        "thursday_flag = ${now.dayOfWeek.equals(DayOfWeek.THURSDAY)}, " +
        "friday_flag = ${now.dayOfWeek.equals(DayOfWeek.FRIDAY)}, " +
        "saturday_flag = ${now.dayOfWeek.equals(DayOfWeek.SATURDAY)}, " +
        "sunday_flag = ${now.dayOfWeek.equals(DayOfWeek.SUNDAY)}",
    )
  }

  private fun updateToRunOnBankHolidays(runsOnBankHoliday: Boolean = true) {
    // Today is stubbed as a bank holiday via the BankHolidayExtension class, via the IntegrationTestBase class.
    jdbcTemplate.update("update activity_schedule set runs_on_bank_holiday = $runsOnBankHoliday")
  }
}
