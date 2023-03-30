package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AuditRecordSearchFilters
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.LocalAuditSearchResults

class AuditIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-audit-1.sql",
  )
  @Test
  fun `should fetch all audit events when filters not set`() {
    val auditRecords = webTestClient.getAuditRecords()
    assertThat(auditRecords.content.size).isEqualTo(1)
  }

  private fun WebTestClient.getAuditRecords() =
    post()
      .uri("/audit/search")
      .bodyValue(AuditRecordSearchFilters())
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LocalAuditSearchResults::class.java)
      .returnResult().responseBody
}
