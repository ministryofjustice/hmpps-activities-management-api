package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceSync
import java.time.LocalDate

class AttendanceSyncIntegrationTest : IntegrationTestBase() {
  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return attendance sync`() {
    val attendanceSync =
      webTestClient.get()
        .uri("/synchronisation/attendance/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(AttendanceSync::class.java)
        .returnResult().responseBody!!

    with(attendanceSync) {
      assertThat(attendanceId).isEqualTo(1)
      assertThat(scheduledInstanceId).isEqualTo(1)
      assertThat(activityScheduleId).isEqualTo(1)
      assertThat(sessionDate).isEqualTo(LocalDate.now())
      assertThat(sessionStartTime).isEqualTo("10:00")
      assertThat(sessionEndTime).isEqualTo("11:00")
      assertThat(prisonerNumber).isEqualTo("A22222A")
      assertThat(bookingId).isEqualTo(10002)
      assertThat(attendanceReasonCode).isEqualTo("SICK")
      assertThat(comment).isEqualTo("Attendance Comment")
      assertThat(status).isEqualTo("WAITING")
      assertThat(payAmount).isEqualTo(150)
      assertThat(bonusAmount).isEqualTo(50)
      assertThat(issuePayment).isTrue
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return unauthorised`() {
    webTestClient.get()
      .uri("/synchronisation/attendance/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return forbidden if no role`() {
    webTestClient.get()
      .uri("/synchronisation/attendance/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/synchronisation/attendance/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("INVALID_ROLE")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return not found`() {
    webTestClient.get()
      .uri("/synchronisation/attendance/1111")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return bad request`() {
    webTestClient.get()
      .uri("/synchronisation/attendance/INVALID")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isBadRequest
  }
}
