package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.DayOfWeek
import java.time.LocalDate

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.jobs.sqs.schedules.enabled=true",
  ],
)
class CreateScheduledInstancesJobSqsIntegrationTest : AbstractJobIntegrationTest() {

  @MockitoBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var scheduleInstancesRepository: ScheduledInstanceRepository

  private val today: LocalDate = TimeSource.today()

  @Sql("classpath:test_data/seed-scheduled-instances-for-multiple-prisons.sql")
  @Test
  fun `creates instances for multiple prisons`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(true)

    waitForJobs({ webTestClient.createScheduledInstances() })

    val scheduledInstances = scheduleInstancesRepository.findAll()

    with(scheduledInstances) {
      assertThat(this).hasSize(2)
      assertThat(scheduledInstances.filter { it.activitySchedule.activity.prisonCode == "PVI" }).hasSize(1)
      assertThat(scheduledInstances.filter { it.activitySchedule.activity.prisonCode == "IWI" }).hasSize(1)
    }

    assertThat(scheduleInstancesRepository.count()).isEqualTo(2)

    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, 1L)
    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, 2L)
    verifyNoMoreInteractions(outboundEventsService)

    verifyJobComplete(JobType.SCHEDULES)
  }

  @Sql("classpath:test_data/seed-activity-with-old-instances.sql")
  @Test
  fun `creates instances for activity sessions on bank holidays and for suspended schedules`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(true)

    waitForJobs({ webTestClient.createScheduledInstances() })

    val scheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date >= '$today'",
    )

    scheduledInstances isEqualTo 1

    val allInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where activity_schedule_id = 1",
    )

    allInstances isEqualTo 3

    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, 1L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Sql("classpath:test_data/seed-activity-with-old-instances.sql")
  @Test
  fun `do not create instances for activity sessions when not marked as running on a bank holiday`() {
    updateSlotsToRunToday()
    updateToRunOnBankHolidays(false)

    waitForJobs({ webTestClient.createScheduledInstances() })

    val scheduledInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$today'",
    )

    scheduledInstances isEqualTo 0

    verifyNoMoreInteractions(outboundEventsService)
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

    waitForJobs({ webTestClient.createScheduledInstances() })

    val createdForToday = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where session_date = '$today'",
    )

    createdForToday isEqualTo 1

    val allInstances = jdbcTemplate.queryForObject<Long>(
      "select count(*) from scheduled_instance where activity_schedule_id = 1",
    )

    allInstances isEqualTo 3

    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, 1L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  private fun WebTestClient.createScheduledInstances() {
    post()
      .uri("/job/create-scheduled-instances")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
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
