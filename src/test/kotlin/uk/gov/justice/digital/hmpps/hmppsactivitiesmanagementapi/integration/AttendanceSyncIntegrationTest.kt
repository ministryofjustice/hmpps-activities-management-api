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
  fun `should return attendance sync for a completed attendance`() {
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
      assertThat(comment).isEqualTo("Sick - Unpaid - Attendance Comment")
      assertThat(status).isEqualTo("COMPLETED")
      assertThat(payAmount).isEqualTo(150)
      assertThat(bonusAmount).isEqualTo(50)
      assertThat(issuePayment).isFalse()
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return attendance sync for a waiting attendance`() {
    val attendanceSync =
      webTestClient.get()
        .uri("/synchronisation/attendance/2")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(AttendanceSync::class.java)
        .returnResult().responseBody!!

    with(attendanceSync) {
      assertThat(attendanceId).isEqualTo(2)
      assertThat(scheduledInstanceId).isEqualTo(1)
      assertThat(activityScheduleId).isEqualTo(1)
      assertThat(sessionDate).isEqualTo(LocalDate.now())
      assertThat(sessionStartTime).isEqualTo("10:00")
      assertThat(sessionEndTime).isEqualTo("11:00")
      assertThat(prisonerNumber).isEqualTo("A11111A")
      assertThat(bookingId).isEqualTo(10001)
      assertThat(attendanceReasonCode).isNull()
      assertThat(comment).isNull()
      assertThat(status).isEqualTo("WAITING")
      assertThat(payAmount).isNull()
      assertThat(bonusAmount).isNull()
      assertThat(issuePayment).isNull()
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return attendance sync where prisoner has been deallocated and reallocated`() {
    val attendanceSync =
      webTestClient.get()
        .uri("/synchronisation/attendance/3")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(AttendanceSync::class.java)
        .returnResult().responseBody!!

    with(attendanceSync) {
      assertThat(attendanceId).isEqualTo(3)
      assertThat(scheduledInstanceId).isEqualTo(1)
      assertThat(activityScheduleId).isEqualTo(1)
      assertThat(sessionDate).isEqualTo(LocalDate.now())
      assertThat(sessionStartTime).isEqualTo("10:00")
      assertThat(sessionEndTime).isEqualTo("11:00")
      assertThat(prisonerNumber).isEqualTo("A33333A")
      assertThat(bookingId).isEqualTo(10003)
      assertThat(attendanceReasonCode).isEqualTo("SICK")
      assertThat(comment).isEqualTo("Sick - Unpaid - test comment")
      assertThat(status).isEqualTo("COMPLETED")
      assertThat(payAmount).isEqualTo(200)
      assertThat(bonusAmount).isNull()
      assertThat(issuePayment).isNull()
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-17.sql",
  )
  @Test
  fun `should return correct booking where prisoner has been allocated, ended and reallocated on a different booking id`() {
    val attendanceSync =
      webTestClient.get()
        .uri("/synchronisation/attendance/4")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(AttendanceSync::class.java)
        .returnResult().responseBody!!

    with(attendanceSync) {
      assertThat(attendanceId).isEqualTo(4)
      assertThat(bookingId).isEqualTo(10005)
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
