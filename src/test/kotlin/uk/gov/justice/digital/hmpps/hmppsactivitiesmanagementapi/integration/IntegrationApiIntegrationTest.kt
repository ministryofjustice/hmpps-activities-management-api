package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_HMPPS_INTEGRATION_API
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.prisoner.attendance-created=true",
    "feature.event.activities.prisoner.attendance-amended=true",
  ],
)
class IntegrationApiIntegrationTest : ActivitiesIntegrationTestBase() {

  @Nested
  inner class GetAttendances {

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `get prisoner attendance without prison code`() {
      val prisonerNumber = "A11111A"

      val attendanceList = webTestClient.getAttendanceForPrisoner(
        prisonerNumber = prisonerNumber,
        startDate = LocalDate.of(2022, 10, 10),
        endDate = LocalDate.of(2022, 10, 11),
      )

      assertThat(attendanceList.size).isEqualTo(5)
      assertThat(attendanceList.first().prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(attendanceList.first().scheduleInstanceId).isEqualTo(1)
      assertThat(attendanceList.first().attendanceReason).isNull()
      assertThat(attendanceList.first().comment).isNull()
    }

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `get prisoner attendance with prison code`() {
      val prisonerNumber = "A11111A"

      val attendanceList = webTestClient.getAttendanceForPrisoner(
        prisonCode = MOORLAND_PRISON_CODE,
        prisonerNumber = prisonerNumber,
        startDate = LocalDate.of(2022, 10, 10),
        endDate = LocalDate.of(2022, 10, 11),
      )

      assertThat(attendanceList.size).isEqualTo(5)
      assertThat(attendanceList.first().prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(attendanceList.first().scheduleInstanceId).isEqualTo(1)
      assertThat(attendanceList.first().attendanceReason).isNull()
      assertThat(attendanceList.first().comment).isNull()
    }

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `does not get prisoner attendance when no data in date range`() {
      val prisonerNumber = "A11111A"

      val attendanceList = webTestClient.getAttendanceForPrisoner(
        prisonCode = MOORLAND_PRISON_CODE,
        prisonerNumber = prisonerNumber,
        startDate = LocalDate.of(2022, 12, 10),
        endDate = LocalDate.of(2022, 12, 11),
      )

      assertThat(attendanceList.size).isEqualTo(0)
    }

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `get prisoner attendance with invalid prison code`() {
      val prisonerNumber = "A11111A"

      webTestClient.get()
        .uri("/integration-api/attendances/$prisonerNumber?startDate=2022-10-10&endDate=2022-10-11&prisonCode=ABC")
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `get prisoner attendance returns bad request when no dates supplied`() {
      val prisonerNumber = "A11111A"

      webTestClient.get()
        .uri("/integration-api/attendances/$prisonerNumber")
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
    }

    @Test
    fun `get prisoner attendance returns bad request when dates greater than 4 weeks apart supplied`() {
      val prisonerNumber = "A11111A"

      webTestClient.get()
        .uri("/integration-api/attendances/$prisonerNumber?startDate=2022-10-10&endDate=2022-12-11")
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
    }

    private fun WebTestClient.getAttendanceForPrisoner(
      prisonCode: String? = null,
      startDate: LocalDate,
      endDate: LocalDate,
      prisonerNumber: String,
    ) = get()
      .uri("/integration-api/attendances/$prisonerNumber?startDate=$startDate&endDate=$endDate${prisonCode?.let { "&prisonCode=$it" } ?: ""}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ModelAttendance::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("getScheduledInstancesForPrisoner")
  inner class GetScheduledInstancesForPrisoner {
    val prisonerNumber = "A11111A"

    @Test
    @Sql("classpath:test_data/seed-activity-integration-api-1.sql")
    fun `returns data within the time slot AND which have the correct prisoner number`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesForPrisonerBy(
          prisonerNumber = prisonerNumber,
          prisonCode = MOORLAND_PRISON_CODE,
          startDate = startDate,
          endDate = endDate,
        )

      assertThat(scheduledInstances).isNotNull()
      assertThat(scheduledInstances).hasSize(6)
      val attendances = scheduledInstances?.flatMap { it.attendances }
      assertThat(attendances).allMatch { it.prisonerNumber == prisonerNumber }
      val advanceAttendances = scheduledInstances?.flatMap { it.advanceAttendances }
      assertThat(advanceAttendances).allMatch { it.prisonerNumber == prisonerNumber }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-integration-api-1.sql")
    fun `returns data with the time slot filter`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesForPrisonerBy(
          prisonerNumber = prisonerNumber,
          prisonCode = MOORLAND_PRISON_CODE,
          startDate = startDate,
          endDate = endDate,
          timeSlot = TimeSlot.AM,
        )

      assertThat(scheduledInstances).hasSize(4)
      val attendances = scheduledInstances?.flatMap { it.attendances }
      assertThat(attendances).allMatch { it.prisonerNumber == prisonerNumber }
      val advanceAttendances = scheduledInstances?.flatMap { it.advanceAttendances }
      assertThat(advanceAttendances).allMatch { it.prisonerNumber == prisonerNumber }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-integration-api-1.sql")
    fun `returns data in the date range with the correct prisoner number`() {
      val startDate = LocalDate.of(2022, 10, 2)
      val endDate = LocalDate.of(2022, 11, 4)

      val scheduledInstances =
        webTestClient.getScheduledInstancesForPrisonerBy(
          prisonerNumber = prisonerNumber,
          prisonCode = MOORLAND_PRISON_CODE,
          startDate = startDate,
          endDate = endDate,
        )

      assertThat(scheduledInstances).hasSize(4)
      val attendances = scheduledInstances?.flatMap { it.attendances }
      assertThat(attendances).allMatch { it.prisonerNumber == prisonerNumber }
      val advanceAttendances = scheduledInstances?.flatMap { it.advanceAttendances }
      assertThat(advanceAttendances).allMatch { it.prisonerNumber == prisonerNumber }
    }

    private fun WebTestClient.getScheduledInstancesForPrisonerBy(
      prisonerNumber: String,
      prisonCode: String,
      startDate: LocalDate,
      endDate: LocalDate,
      timeSlot: TimeSlot? = null,
    ) = get()
      .uri { builder ->
        builder
          .path("/integration-api/prisons/$prisonCode/$prisonerNumber/scheduled-instances")
          .queryParam("startDate", startDate)
          .queryParam("endDate", endDate)
          .maybeQueryParam("slot", timeSlot)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody
  }
}
