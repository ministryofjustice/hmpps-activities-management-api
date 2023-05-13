package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EventReviewSearchResults
import java.time.LocalDate

class EventReviewIntegrationTest : IntegrationTestBase() {

  @Sql("classpath:test_data/event-review-data.sql")
  @Test
  fun `should return rows with default filter settings`() {
    val result = webTestClient.getEvents()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    // Expect pageSize 10, ascending time order
    with(result!!) {
      assertThat(content.size).isEqualTo(10)
      assertThat(content.first().eventReviewId).isEqualTo(1)
      assertThat(content.last().eventReviewId).isEqualTo(10)
      assertThat(content.first().eventTime).isBefore(content.last().eventTime)
      assertThat(totalPages).isEqualTo(2)
    }

    // Should not include any acknowledged events
    result.content.map {
      assertThat(it.acknowledgedTime).isNull()
    }
  }

  @Sql("classpath:test_data/event-review-data.sql")
  @Test
  fun `should paginate events`() {
    var result = webTestClient.getEvents(page = 0, size = 2)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    with(result!!) {
      assertThat(content.size).isEqualTo(2)
      assertThat(totalPages).isEqualTo(6)
    }

    result = webTestClient.getEvents(page = 0, size = 5)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    with(result!!) {
      assertThat(content.size).isEqualTo(5)
      assertThat(totalPages).isEqualTo(3)
    }

    result = webTestClient.getEvents(page = 0, size = 12)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    with(result!!) {
      assertThat(content.size).isEqualTo(12)
      assertThat(totalPages).isEqualTo(1)
    }
  }

  @Sql("classpath:test_data/event-review-data.sql")
  @Test
  fun `should include acknowledged events when requested`() {
    val result = webTestClient.getEvents(includeAcknowledged = true, size = 13)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    with(result!!) {
      assertThat(content.size).isEqualTo(13)
      assertThat(totalPages).isEqualTo(1)
    }
  }

  @Sql("classpath:test_data/event-review-data.sql")
  @Test
  fun `should filter by prisoner number when requested`() {
    val result = webTestClient.getEvents(prisonerNumber = "A1234AA")
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    with(result!!) {
      assertThat(content.size).isEqualTo(5)
      assertThat(totalPages).isEqualTo(1)
    }
  }

  @Sql("classpath:test_data/event-review-data.sql")
  @Test
  fun `should sort in descending event time order`() {
    val result = webTestClient.getEvents(sort = "descending", page = 0, size = 12)
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EventReviewSearchResults::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull

    with(result!!) {
      assertThat(content.first().eventReviewId).isEqualTo(12)
      assertThat(content.last().eventReviewId).isEqualTo(1)
      assertThat(totalPages).isEqualTo(1)
    }
  }

  @Sql("classpath:test_data/event-review-data.sql")
  @Test
  fun `should fail with forbidden - incorrect role`() {
    webTestClient.getEvents(role = "INVALID_ROLE")
      .expectStatus().isForbidden
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
  }

  private fun WebTestClient.getEvents(
    date: LocalDate = LocalDate.of(2023, 5, 10),
    prisonCode: String? = "MDI",
    includeAcknowledged: Boolean? = null,
    sort: String? = null,
    page: Long? = null,
    size: Long? = null,
    prisonerNumber: String? = null,
    role: String = "ROLE_ACTIVITY_ADMIN",
  ) = get().uri { builder ->
    builder
      .path("/event-review/prison/$prisonCode")
      .queryParam("date", date)
      .maybeQueryParam("size", size)
      .maybeQueryParam("page", page)
      .maybeQueryParam("includeAcknowledged", includeAcknowledged)
      .maybeQueryParam("sortDirection", sort)
      .maybeQueryParam("prisonerNumber", prisonerNumber)
      .build()
  }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(role)))
    .exchange()
}
