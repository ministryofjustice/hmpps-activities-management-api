package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate

class AttendanceReconIntegrationTest : IntegrationTestBase() {

  private val yesterday = LocalDate.now().minusDays(1)

  @Sql("classpath:test_data/seed-activity-id-27.sql")
  @Test
  fun `should return unauthorised`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/attendances/PVI?date=$yesterday")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql("classpath:test_data/seed-activity-id-27.sql")
  @Test
  fun `should return forbidden if no role`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/attendances/PVI?date=$yesterday")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-27.sql")
  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/attendances/PVI?date=$yesterday")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("INVALID_ROLE")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-27.sql")
  @Test
  fun `should return bad request if no date passed`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/attendances/PVI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Sql("classpath:test_data/seed-activity-id-27.sql")
  @Test
  fun `should return empty list if none`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/attendances/RSI?date=$yesterday")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("prisonCode").isEqualTo("RSI")
      .jsonPath("date").isEqualTo("$yesterday")
      .jsonPath("bookings.size()").isEqualTo(0)
  }

  @Sql("classpath:test_data/seed-activity-id-27.sql")
  @Test
  fun `should return booking counts for the passed prison`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/attendances/PVI?date=$yesterday")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("prisonCode").isEqualTo("PVI")
      .jsonPath("date").isEqualTo("$yesterday")
      .jsonPath("bookings.size()").isEqualTo(3)
      // prisoner A11111A ignores attendance_id 1 and 3 because they're on the wrong date
      .jsonPath("bookings[0].bookingId").isEqualTo("10001")
      .jsonPath("bookings[0].count").isEqualTo("2")
      // prisoner A22222A ignores attendance_id=6 because they're no longer allocated
      .jsonPath("bookings[1].bookingId").isEqualTo("10002")
      .jsonPath("bookings[1].count").isEqualTo("1")
      // prisoner A33333A ignores attendance_id=7 because they weren't paid
      .jsonPath("bookings[2].bookingId").isEqualTo("10003")
      .jsonPath("bookings[2].count").isEqualTo("1")
  }
}
