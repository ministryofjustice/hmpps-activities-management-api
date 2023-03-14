package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UncancelScheduledInstanceRequest
import java.time.LocalDate

class ActivityScheduleInstanceIntegrationTest : IntegrationTestBase() {

  @Nested
  @DisplayName("getScheduledInstancesById")
  inner class GetScheduledInstancesById {
    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `success`() {
      val scheduledInstance = webTestClient.getScheduledInstancesById(1)

      assertThat(scheduledInstance?.id).isEqualTo(1L)
      assertThat(scheduledInstance?.startTime.toString()).isEqualTo("10:00")
      assertThat(scheduledInstance?.endTime.toString()).isEqualTo("11:00")
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstances")
  inner class GetActivityScheduleInstances {
    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns all 20 rows without the time slot`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate)

      assertThat(scheduledInstances).hasSize(20)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns 10 rows with the time slot filter`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.AM)

      assertThat(scheduledInstances).hasSize(10)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `date range precludes 4 rows from the sample of 20`() {
      val startDate = LocalDate.of(2022, 10, 2)
      val endDate = LocalDate.of(2022, 11, 4)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate)

      assertThat(scheduledInstances).hasSize(16)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-10.sql")
    fun `returns instance when no allocations`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.AM)

      assertThat(scheduledInstances).hasSize(1)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-10.sql")
    fun `returns no instance when match on time slot`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.PM)

      assertThat(scheduledInstances).isEmpty()
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstance")
  inner class UncancelScheduledInstance {

    @Test
    @Sql("classpath:test_data/seed-activity-id-11.sql")
    fun `success`() {
      // webTestClient.uncancelScheduledInstance()
    }
  }

  private fun WebTestClient.uncancelScheduledInstance(id: Long, username: String, displayName: String) = put()
    .uri("/scheduled-instances/$id/uncancel")
    .bodyValue(UncancelScheduledInstanceRequest(username, displayName))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf()))
    .exchange()

  private fun WebTestClient.getScheduledInstancesById(id: Long) = get()
    .uri("/scheduled-instances/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf()))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ActivityScheduleInstance::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getScheduledInstancesBy(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    timeSlot: TimeSlot? = null,
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
