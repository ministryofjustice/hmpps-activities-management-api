package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql

class AllocationReconIntegrationTest : IntegrationTestBase() {
  @Sql("classpath:test_data/seed-activity-id-25.sql")
  @Test
  fun `should return unauthorised`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/allocations/PVI")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql("classpath:test_data/seed-activity-id-25.sql")
  @Test
  fun `should return forbidden if no role`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/allocations/PVI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-25.sql")
  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/allocations/PVI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("INVALID_ROLE")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-25.sql")
  @Test
  fun `should return empty list if none`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/allocations/RSI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("prisonCode").isEqualTo("RSI")
      .jsonPath("bookings.size()").isEqualTo(0)
  }

  @Sql("classpath:test_data/seed-activity-id-25.sql")
  @Test
  fun `should return booking counts for the passed prison`() {
    webTestClient.get()
      .uri("/synchronisation/reconciliation/allocations/PVI")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("prisonCode").isEqualTo("PVI")
      .jsonPath("bookings.size()").isEqualTo(3)
      // prisoner A11111A ignores allocation_id=6 which has ENDED
      .jsonPath("bookings[0].bookingId").isEqualTo("10001")
      .jsonPath("bookings[0].count").isEqualTo("2")
      // prisoner A22222A ignores allocation_id=5 which is suspended
      .jsonPath("bookings[1].bookingId").isEqualTo("10002")
      .jsonPath("bookings[1].count").isEqualTo("1")
      // prisoner A33333A ignores allocation_id=3 from booking 10003 which has ended and ignores allocation_id=8 which is PENDING
      .jsonPath("bookings[2].bookingId").isEqualTo("10004")
      .jsonPath("bookings[2].count").isEqualTo("1")
  }
}
