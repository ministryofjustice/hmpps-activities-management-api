package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

class WaitingListApplicationIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `get waiting list application by id`() {
    assertThat(webTestClient.getById(1)).isNotNull()
  }

  private fun WebTestClient.getById(id: Long) =
    get()
      .uri("/waiting-list-applications/$id")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, moorlandPrisonCode)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(WaitingListApplication::class.java)
      .returnResult().responseBody

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `update waiting list application`() {
    val beforeUpdate = waitingListRepository.findOrThrowNotFound(1)

    with(beforeUpdate) {
      applicationDate isEqualTo TimeSource.today()
      requestedBy isEqualTo "Bob"
      comments isEqualTo "Bob left some comments"
      status isEqualTo WaitingListStatus.PENDING
    }

    val response = webTestClient.update(
      1,
      WaitingListApplicationUpdateRequest(
        applicationDate = TimeSource.yesterday(),
        requestedBy = "Fred",
        comments = "Fred left some comments",
        status = WaitingListStatus.APPROVED,
      ),
    )!!

    val afterUpdate = waitingListRepository.findOrThrowNotFound(1)

    with(afterUpdate) {
      applicationDate isEqualTo TimeSource.yesterday()
      requestedBy isEqualTo "Fred"
      comments isEqualTo "Fred left some comments"
      status isEqualTo WaitingListStatus.APPROVED

      applicationDate isEqualTo response.requestedDate
      requestedBy isEqualTo response.requestedBy
      comments isEqualTo response.comments
      status isEqualTo response.status
    }
  }

  private fun WebTestClient.update(id: Long, request: WaitingListApplicationUpdateRequest) =
    webTestClient.patch()
      .uri("/waiting-list-applications/$id")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, moorlandPrisonCode)
      .bodyValue(request)
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(WaitingListApplication::class.java)
      .returnResult().responseBody
}
