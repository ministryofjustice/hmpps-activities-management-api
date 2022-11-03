package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import java.time.LocalDate
import java.time.LocalTime

class ActivityScheduleInstanceIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-3.sql"
  )
  @Test
  fun `getActivityScheduleInstances for prisoner - returns all 10 rows that satisfy the criteria`() {

    val scheduledInstances =
      webTestClient.getActivityScheduleInstancesForPrisoner(
        "MDI",
        "A11111A",
        LocalDate.of(2022, 10, 1),
        LocalDate.of(2022, 11, 5)
      )
    `assert the schedule instances activities for prisoner A11111A match seed-activity-id-3 sql data`(scheduledInstances)
    `assert the scheduled instance dates and times for prisoner A11111A match seed-activity-id-3 sql`(scheduledInstances)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-3.sql"
  )
  @Test
  fun `getActivityScheduleInstances - returns all 20 rows that satisfy the criteria`() {

    val scheduledInstances =
      webTestClient.getActivityScheduleInstances(
        "MDI",
        LocalDate.of(2022, 10, 1),
        LocalDate.of(2022, 11, 5)
      )
    assertThat(scheduledInstances).hasSize(20)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-3.sql"
  )
  @Test
  fun `getActivityScheduleInstances for prisoner - date range precludes 1 row from each end of the fixture`() {
    val scheduledInstances =
      webTestClient.getActivityScheduleInstancesForPrisoner(
        "MDI",
        "A11111A",
        LocalDate.of(2022, 10, 2),
        LocalDate.of(2022, 11, 4)
      )
    assertThat(scheduledInstances).hasSize(8)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-3.sql"
  )
  @Test
  fun `getActivityScheduleInstances - date range precludes 4 rows from each end of the fixture`() {
    val scheduledInstances =
      webTestClient.getActivityScheduleInstances(
        "MDI",
        LocalDate.of(2022, 10, 2),
        LocalDate.of(2022, 11, 4)
      )
    assertThat(scheduledInstances).hasSize(16)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-3.sql"
  )
  @Test
  fun `getActivityScheduleInstances - wrong prison code will filter results`() {
    val scheduledInstances =
      webTestClient.getActivityScheduleInstancesForPrisoner(
        "PVI",
        "A11111A",
        LocalDate.of(2022, 10, 2),
        LocalDate.of(2022, 11, 4)
      )
    assertThat(scheduledInstances).hasSize(0)
  }

  private fun WebTestClient.getActivityScheduleInstancesForPrisoner(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get()
      .uri("/prisons/$prisonCode/scheduled-instances?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getActivityScheduleInstances(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get()
      .uri("/prisons/$prisonCode/scheduled-instances?startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody
}

private fun `assert the scheduled instance dates and times for prisoner A11111A match seed-activity-id-3 sql`(
  scheduledInstances: List<ActivityScheduleInstance>?
) {

  assertThat(scheduledInstances)
    .extracting<Tuple> { tuple(it.date, it.startTime, it.endTime) }
    .contains(
      tuple(LocalDate.of(2022, 10, 1), LocalTime.of(10, 1, 0), LocalTime.of(11, 0, 0)),
      tuple(LocalDate.of(2022, 10, 2), LocalTime.of(10, 1, 0), LocalTime.of(11, 0, 0)),
      tuple(LocalDate.of(2022, 10, 3), LocalTime.of(10, 1, 0), LocalTime.of(11, 0, 0)),
      tuple(LocalDate.of(2022, 10, 4), LocalTime.of(10, 1, 0), LocalTime.of(11, 0, 0)),
      tuple(LocalDate.of(2022, 10, 5), LocalTime.of(10, 1, 0), LocalTime.of(11, 0, 0)),

      tuple(LocalDate.of(2022, 11, 1), LocalTime.of(14, 1, 0), LocalTime.of(15, 0, 0)),
      tuple(LocalDate.of(2022, 11, 2), LocalTime.of(14, 1, 0), LocalTime.of(15, 0, 0)),
      tuple(LocalDate.of(2022, 11, 3), LocalTime.of(14, 1, 0), LocalTime.of(15, 0, 0)),
      tuple(LocalDate.of(2022, 11, 4), LocalTime.of(14, 1, 0), LocalTime.of(15, 0, 0)),
      tuple(LocalDate.of(2022, 11, 5), LocalTime.of(14, 1, 0), LocalTime.of(15, 0, 0)),
    )
}

private fun `assert the schedule instances activities for prisoner A11111A match seed-activity-id-3 sql data`(
  scheduledInstances: List<ActivityScheduleInstance>?
) {
  assertThat(scheduledInstances).hasSize(10)
  assertThat(scheduledInstances?.get(0)?.activitySchedule?.description).isEqualTo("Geography AM")
  assertThat(scheduledInstances?.get(1)?.activitySchedule?.description).isEqualTo("Geography AM")
  assertThat(scheduledInstances?.get(2)?.activitySchedule?.description).isEqualTo("Geography AM")
  assertThat(scheduledInstances?.get(3)?.activitySchedule?.description).isEqualTo("Geography AM")
  assertThat(scheduledInstances?.get(4)?.activitySchedule?.description).isEqualTo("Geography AM")
  assertThat(scheduledInstances?.get(5)?.activitySchedule?.description).isEqualTo("English PM")
  assertThat(scheduledInstances?.get(6)?.activitySchedule?.description).isEqualTo("English PM")
  assertThat(scheduledInstances?.get(7)?.activitySchedule?.description).isEqualTo("English PM")
  assertThat(scheduledInstances?.get(8)?.activitySchedule?.description).isEqualTo("English PM")
  assertThat(scheduledInstances?.get(9)?.activitySchedule?.description).isEqualTo("English PM")
}
