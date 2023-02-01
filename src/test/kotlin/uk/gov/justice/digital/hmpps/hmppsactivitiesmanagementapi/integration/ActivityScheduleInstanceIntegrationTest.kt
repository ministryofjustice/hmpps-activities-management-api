package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import java.time.LocalDate

class ActivityScheduleInstanceIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `get scheduled instance by ID`() {
    val scheduledInstance = webTestClient
      .get()
      .uri("/scheduled-instances/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivityScheduleInstance::class.java)
      .returnResult().responseBody

    assertThat(scheduledInstance?.id).isEqualTo(1L)
    assertThat(scheduledInstance?.startTime.toString()).isEqualTo("10:00")
    assertThat(scheduledInstance?.endTime.toString()).isEqualTo("11:00")
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  fun `getActivityScheduleInstances - returns all 20 rows without the time slot`() {
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate)

    assertThat(scheduledInstances).hasSize(20)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  fun `getActivityScheduleInstances - returns 10 rows with the time slot filter`() {
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.AM)

    assertThat(scheduledInstances).hasSize(10)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  fun `getActivityScheduleInstances - date range precludes 4 rows from the sample of 20`() {
    val startDate = LocalDate.of(2022, 10, 2)
    val endDate = LocalDate.of(2022, 11, 4)

    val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate)

    assertThat(scheduledInstances).hasSize(16)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-10.sql")
  fun `getActivityScheduleInstances - returns instance when no allocations`() {
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.AM)

    assertThat(scheduledInstances).hasSize(1)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-10.sql")
  fun `getActivityScheduleInstances - returns no instance when match on time slot`() {
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.PM)

    assertThat(scheduledInstances).isEmpty()
  }

  private fun WebTestClient.getScheduledInstancesBy(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    timeSlot: TimeSlot? = null
  ) =
    get()
      .uri { builder ->
        builder
          .path("/prisons/$prisonCode/scheduled-instances")
          .queryParam("startDate", startDate)
          .queryParam("endDate", endDate)
          .maybeQueryParam("slot", timeSlot)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody
}
