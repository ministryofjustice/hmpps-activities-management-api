package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID

class WaitingListApplicationIntegrationTest : IntegrationTestBase() {

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `get waiting list application by id`() {
    assertThat(webTestClient.getById(1)).isNotNull()
  }

  private fun WebTestClient.getById(id: Long) =
    get()
      .uri("/waiting-list-applications/$id")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf("ROLE_ACTIVITY_HUB")))
      .header(CASELOAD_ID, moorlandPrisonCode)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(WaitingListApplication::class.java)
      .returnResult().responseBody
}
