package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
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
    val prisonCode = "MDI"
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)

    val scheduledInstances = webTestClient.get()
      .uri("/prisons/$prisonCode/scheduled-instances?startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody

    assertThat(scheduledInstances).hasSize(20)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  fun `getActivityScheduleInstances - returns 10 rows with the time slot filter`() {
    val prisonCode = "MDI"
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)
    val timeSlot = "am"

    val scheduledInstances = webTestClient.get()
      .uri("/prisons/$prisonCode/scheduled-instances?startDate=$startDate&endDate=$endDate&slot=$timeSlot")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody

    assertThat(scheduledInstances).hasSize(10)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  fun `getActivityScheduleInstances - date range precludes 4 rows from the sample of 20`() {
    val prisonCode = "MDI"
    val startDate = LocalDate.of(2022, 10, 2)
    val endDate = LocalDate.of(2022, 11, 4)

    val scheduledInstances = webTestClient.get()
      .uri("/prisons/$prisonCode/scheduled-instances?startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody

    assertThat(scheduledInstances).hasSize(16)
  }
}
