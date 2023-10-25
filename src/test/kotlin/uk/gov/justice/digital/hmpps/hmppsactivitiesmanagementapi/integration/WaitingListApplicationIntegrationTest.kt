package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate

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

  @Sql("classpath:test_data/seed-activity-id-26.sql")
  @Test
  fun `search all waiting list applications`() {
    val results = webTestClient.searchWaitingLists("MDI", WaitingListSearchRequest())

    results["empty"] isEqualTo false
    results["totalElements"] isEqualTo 5

    val content = results["content"] as List<LinkedHashMap<String, Any>>

    with(content[0]) {
      this["id"] isEqualTo 1
      this["prisonerNumber"] isEqualTo "ABCD01"
      this["status"] isEqualTo "PENDING"
      this["activityId"] isEqualTo 1
    }

    with(content[1]) {
      this["id"] isEqualTo 2
      this["prisonerNumber"] isEqualTo "ABCD02"
      this["status"] isEqualTo "APPROVED"
      this["activityId"] isEqualTo 1
    }

    with(content[2]) {
      this["id"] isEqualTo 3
      this["prisonerNumber"] isEqualTo "ABCD03"
      this["status"] isEqualTo "PENDING"
      this["activityId"] isEqualTo 1
    }

    with(content[3]) {
      this["id"] isEqualTo 4
      this["prisonerNumber"] isEqualTo "ABCD04"
      this["status"] isEqualTo "PENDING"
      this["activityId"] isEqualTo 2
    }

    with(content[4]) {
      this["id"] isEqualTo 5
      this["prisonerNumber"] isEqualTo "ABCD05"
      this["status"] isEqualTo "PENDING"
      this["activityId"] isEqualTo 1
    }
  }

  @Sql("classpath:test_data/seed-activity-id-26.sql")
  @Test
  fun `search waiting list applications with filters`() {
    val request = WaitingListSearchRequest(
      applicationDateFrom = LocalDate.parse("2023-02-01"),
      applicationDateTo = LocalDate.parse("2023-12-01"),
      activityId = 1,
      prisonerNumbers = listOf("ABCD03", "ABCD04", "ABCD05"),
      status = listOf(WaitingListStatus.PENDING),
    )

    val results = webTestClient.searchWaitingLists("MDI", request)

    results["empty"] isEqualTo false
    results["totalElements"] isEqualTo 2

    val content = results["content"] as List<LinkedHashMap<String, Any>>

    with(content[0]) {
      this["id"] isEqualTo 3
    }

    with(content[1]) {
      this["id"] isEqualTo 5
    }
  }

  private fun WebTestClient.searchWaitingLists(
    prisonCode: String,
    request: WaitingListSearchRequest,
  ): LinkedHashMap<String, Any> =
    post().uri("/waiting-list-applications/$prisonCode/search")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, prisonCode)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(typeReference<LinkedHashMap<String, Any>>())
      .returnResult().responseBody!!
}
