package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ScheduleCreatedInformation
import java.time.DayOfWeek
import java.time.LocalDate

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.activity-schedule.amended=true",
  ],
)
class CreateScheduledInstancesJobIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  private val today: LocalDate = TimeSource.today()

  @Sql("classpath:test_data/seed-activity-with-old-instances.sql")
  @Test
  fun `creates instances for activity sessions on bank holidays and for suspended schedules`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(true)

    webTestClient.createScheduledInstances()

    val scheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date >= '$today'",
    )

    scheduledInstances isEqualTo 1

    val allInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where activity_schedule_id = 1",
    )

    allInstances isEqualTo 3

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      eventType isEqualTo "activities.activity-schedule.amended"
      additionalInformation isEqualTo ScheduleCreatedInformation(1)
    }

    eventCaptor.allValues hasSize 1
  }

  @Sql("classpath:test_data/seed-activity-with-old-instances.sql")
  @Test
  fun `Do not create instances for activity sessions when not marked as running on a bank holiday`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(false)

    webTestClient.createScheduledInstances()

    val scheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$today'",
    )

    scheduledInstances isEqualTo 0

    verifyNoInteractions(eventsPublisher)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-with-old-instances.sql")
  fun `old instances are filtered and leaves the past intact when creating new sessions`() {
    val yesterday: LocalDate = LocalDate.now().minusDays(1)
    val dayBeforeYesterday: LocalDate = LocalDate.now().minusDays(2)
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(true)

    // Update the two existing instances to be (just) in the past and the job should leave these unchanged.
    jdbcTemplate.update("update scheduled_instance set session_date = '$dayBeforeYesterday' where session_date = '2022-10-10'")
    jdbcTemplate.update("update scheduled_instance set session_date = '$yesterday' where session_date = '2022-10-11'")

    webTestClient.createScheduledInstances()

    val createdForToday = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$today'",
    )

    createdForToday isEqualTo 1

    val allInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where activity_schedule_id = 1",
    )

    allInstances isEqualTo 3

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      eventType isEqualTo "activities.activity-schedule.amended"
      additionalInformation isEqualTo ScheduleCreatedInformation(1)
    }

    eventCaptor.allValues hasSize 1
  }

  private fun currentMaxJobId() = jdbcTemplate.queryForObject<Long>("select max(job_id) from job")

  private fun waitForAllNewJobsCompleteSuccessfully(block: () -> Unit) {
    val oldMaxId = currentMaxJobId()

    block()

    await untilAsserted {
      assertThat(currentMaxJobId()).isGreaterThan(oldMaxId)
      assertThat(jdbcTemplate.queryForObject<Boolean>("select not exists(select 1 from job where successful = false)")).isTrue()
    }
  }

  private fun WebTestClient.createScheduledInstances() {
    waitForAllNewJobsCompleteSuccessfully {
      post()
        .uri("/job/create-scheduled-instances")
        .accept(MediaType.TEXT_PLAIN)
        .exchange()
        .expectStatus().isCreated
    }
  }

  private fun updateSlotsToRunToday() {
    val now = LocalDate.now()
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
