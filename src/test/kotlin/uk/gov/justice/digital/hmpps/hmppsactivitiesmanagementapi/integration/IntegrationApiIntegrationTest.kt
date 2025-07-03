package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
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
}
