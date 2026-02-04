package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.asListOfType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.startsWith
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplicationHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerWaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@TestPropertySource(
  properties = [
    "feature.audit.service.local.enabled=true",
    "feature.audit.service.hmpps.enabled=true",
  ],
)
class WaitingListApplicationIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @Autowired
  private lateinit var activityScheduleRepository: ActivityScheduleRepository

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `get waiting list application by id`() {
    stubPrisoners(listOf("ABCDEF"))
    assertThat(webTestClient.getById(1)).isNotNull()
  }

  private fun WebTestClient.getById(id: Long) = get()
    .uri("/waiting-list-applications/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
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

    val response = update(
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

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `re-instate a withdrawn application`() {
    stubPrisoners(listOf("Z1111ZZ"))

    val beforeUpdate = waitingListRepository.findOrThrowNotFound(2)

    with(beforeUpdate) {
      applicationDate isEqualTo TimeSource.today()
      requestedBy isEqualTo "Jemima"
      comments isEqualTo "Jemima left some comments"
      status isEqualTo WaitingListStatus.WITHDRAWN
    }

    val response = update(
      2,
      WaitingListApplicationUpdateRequest(
        applicationDate = TimeSource.yesterday(),
        requestedBy = "Fred",
        comments = "Fred left some comments",
        status = WaitingListStatus.PENDING,
      ),
    )!!

    val afterUpdate = waitingListRepository.findOrThrowNotFound(2)

    with(afterUpdate) {
      applicationDate isEqualTo TimeSource.yesterday()
      requestedBy isEqualTo "Fred"
      comments isEqualTo "Fred left some comments"
      status isEqualTo WaitingListStatus.PENDING

      applicationDate isEqualTo response.requestedDate
      requestedBy isEqualTo response.requestedBy
      comments isEqualTo response.comments
      status isEqualTo response.status
    }
  }

  private fun update(id: Long, request: WaitingListApplicationUpdateRequest) = webTestClient.patch()
    .uri("/waiting-list-applications/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
    .bodyValue(request)
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(WaitingListApplication::class.java)
    .returnResult().responseBody

  @Sql("classpath:test_data/seed-activity-id-26.sql")
  @Test
  fun `search all waiting list applications`() {
    stubPrisoners(listOf("ABCD01", "ABCD02", "ABCD03", "ABCD04", "ABCD05"))

    val results = webTestClient.searchWaitingLists("BCI", WaitingListSearchRequest())

    results["empty"] isEqualTo false
    results["totalElements"] isEqualTo 5

    val content = (results["content"] as List<*>).asListOfType<LinkedHashMap<String, Any>>()

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

    stubPrisoners(listOf("ABCD03", "ABCD04", "ABCD05"))

    val results = webTestClient.searchWaitingLists("BCI", request)

    results["empty"] isEqualTo false
    results["totalElements"] isEqualTo 2

    val content = (results["content"] as List<*>).asListOfType<LinkedHashMap<String, Any>>()

    with(content[0]) {
      this["id"] isEqualTo 3
    }

    with(content[1]) {
      this["id"] isEqualTo 5
    }
  }

  private fun stubPrisoners(prisonerNumbers: List<String>) {
    prisonerNumbers.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
        PrisonerSearchPrisonerFixture.instance(
          prisonId = MOORLAND_PRISON_CODE,
          prisonerNumber = it,
          bookingId = 1,
          status = "ACTIVE IN",
        ),
      )
    }
  }

  @Sql("classpath:test_data/seed-activity-id-20.sql")
  @Test
  fun `add prisoner to a waiting list for an activity`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("G4793VF")

    val request = WaitingListApplicationRequest(
      prisonerNumber = "G4793VF",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Some comments from Bob",
      status = WaitingListStatus.PENDING,
    )

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()

    webTestClient.waitingListApplication(MOORLAND_PRISON_CODE, request, MOORLAND_PRISON_CODE).expectStatus().isNoContent

    val persisted = waitingListRepository.findAll().also { assertThat(it).hasSize(1) }.first()

    with(persisted) {
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(activitySchedule.activityScheduleId).isEqualTo(1L)
      assertThat(applicationDate).isToday()
      assertThat(requestedBy).isEqualTo("Bob")
      assertThat(comments).isEqualTo("Some comments from Bob")
      assertThat(status).isEqualTo(WaitingListStatus.PENDING)
    }

    val localAuditRecord = auditRepository.findAll().also { assertThat(it).hasSize(1) }.first()

    with(localAuditRecord) {
      auditType isEqualTo uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType.PRISONER
      detailType isEqualTo uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType.PRISONER_ADDED_TO_WAITING_LIST
      activityId isEqualTo 1L
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "G4793VF"
      recordedTime isCloseTo TimeSource.now()
      message startsWith "Prisoner G4793VF was added to the waiting list for activity 'Maths'(1) with a status of PENDING. Event created on ${TimeSource.today()}"
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())

    with(hmppsAuditEventCaptor.firstValue) {
      println(details)
      assertThat(what).isEqualTo("PRISONER_ADDED_TO_WAITING_LIST")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"Maths\",\"prisonCode\":\"MDI\",\"prisonerNumber\":\"G4793VF\",\"scheduleId\":1,\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }
  }

  @Sql("classpath:test_data/seed-activity-id-20.sql")
  @Test
  fun `add prisoner to a waiting list when they have a future allocation that was ended`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("G4793VF")

    val request = WaitingListApplicationRequest(
      prisonerNumber = "G4793VF",
      activityScheduleId = 2L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Some comments from Bob",
      status = WaitingListStatus.PENDING,
    )

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()

    webTestClient.waitingListApplication(MOORLAND_PRISON_CODE, request, MOORLAND_PRISON_CODE).expectStatus().isNoContent

    val persisted = waitingListRepository.findAll().also { assertThat(it).hasSize(1) }.first()

    with(persisted) {
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(activitySchedule.activityScheduleId).isEqualTo(2L)
      assertThat(applicationDate).isToday()
      assertThat(requestedBy).isEqualTo("Bob")
      assertThat(comments).isEqualTo("Some comments from Bob")
      assertThat(status).isEqualTo(WaitingListStatus.PENDING)
    }

    val localAuditRecord = auditRepository.findAll().also { assertThat(it).hasSize(1) }.first()

    with(localAuditRecord) {
      auditType isEqualTo uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType.PRISONER
      detailType isEqualTo uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType.PRISONER_ADDED_TO_WAITING_LIST
      activityId isEqualTo 2L
      prisonCode isEqualTo MOORLAND_PRISON_CODE
      prisonerNumber isEqualTo "G4793VF"
      recordedTime isCloseTo TimeSource.now()
      message startsWith "Prisoner G4793VF was added to the waiting list for activity 'English'(2) with a status of PENDING. Event created on ${TimeSource.today()}"
    }
  }

  private fun createPrisonerWaitingListRequest(size: Int, statuses: List<WaitingListStatus>): List<PrisonerWaitingListApplicationRequest> = List(size) { index ->
    PrisonerWaitingListApplicationRequest(
      activityScheduleId = index + 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Requester",
      comments = "Testing",
      status = statuses.getOrElse(index) { WaitingListStatus.PENDING },
    )
  }

  @Sql("classpath:test_data/seed-activity-add-prisoner-to-multiple-activities.sql")
  @Test
  fun `successfully adds a prisoner to a waiting list for up to 5 activities`() {
    val prisonerNumber = "G4793VF"

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)

    val requestList = createPrisonerWaitingListRequest(
      5,
      listOf(
        WaitingListStatus.PENDING,
        WaitingListStatus.APPROVED,
        WaitingListStatus.DECLINED,
        WaitingListStatus.APPROVED,
        WaitingListStatus.DECLINED,
      ),
    )

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()

    webTestClient.prisonerWaitingListApplication(MOORLAND_PRISON_CODE, prisonerNumber, requestList).expectStatus().isNoContent

    val waitingListEntries = waitingListRepository.findAll()
    assertThat(waitingListEntries).hasSize(5)

    waitingListEntries.forEachIndexed { index, entry ->
      with(entry) {
        assertThat(activitySchedule.activityScheduleId).isEqualTo(index + 1L)
        assertThat(applicationDate).isToday()
        assertThat(requestedBy).isEqualTo("Requester")
        assertThat(comments).isEqualTo("Testing")
        assertThat(status).isEqualTo(requestList[index].status)
        assertThat(prisonCode).isEqualTo(MOORLAND_PRISON_CODE)
      }
    }

    val auditRecords = auditRepository.findAll()
    assertThat(auditRecords).hasSize(5)

    auditRecords.forEachIndexed { index, auditRecord ->
      with(auditRecord) {
        assertThat(auditType).isEqualTo(uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType.PRISONER)
        assertThat(detailType).isEqualTo(uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType.PRISONER_ADDED_TO_WAITING_LIST)
        assertThat(activityId).isEqualTo(index + 1L)
        assertThat(prisonCode).isEqualTo(MOORLAND_PRISON_CODE)
        assertThat(recordedTime).isCloseTo(TimeSource.now(), within(Duration.ofSeconds(60)))
        assertThat(message).startsWith("Prisoner G4793VF was added to the waiting list for activity")
      }
    }
  }

  @Sql("classpath:test_data/seed-activity-add-prisoner-to-multiple-activities.sql")
  @Test
  fun `returns 400 when more than 5 applications are submitted`() {
    val prisonerNumber = "G4793VF"

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)

    val requestList = createPrisonerWaitingListRequest(6, listOf(WaitingListStatus.PENDING))

    webTestClient.prisonerWaitingListApplication(MOORLAND_PRISON_CODE, prisonerNumber, requestList).expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains("A maximum of 5 waiting list application requests can be submitted at once")
      }

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()
  }

  @Sql("classpath:test_data/seed-activity-add-prisoner-to-multiple-activities.sql")
  @Test
  fun `returns 400 when application request is empty`() {
    val prisonerNumber = "G4793VF"

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)

    val requestList = emptyList<PrisonerWaitingListApplicationRequest>()

    webTestClient.prisonerWaitingListApplication(MOORLAND_PRISON_CODE, prisonerNumber, requestList).expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains("At least one waiting list application request must be provided")
      }

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()
  }

  @Sql("classpath:test_data/seed-activity-add-prisoner-to-multiple-activities.sql")
  @Test
  fun `returns 401 when the user is unauthorized`() {
    val prisonerNumber = "G4793VF"

    val requestList = createPrisonerWaitingListRequest(1, listOf(WaitingListStatus.PENDING))
    webTestClient.post()
      .uri("/waiting-list-applications/$MOORLAND_PRISON_CODE/prisoner/$prisonerNumber")
      .bodyValue(requestList)
      .accept(MediaType.APPLICATION_JSON)
      .header(CASELOAD_ID, "USERNAME")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql("classpath:test_data/seed-activity-add-prisoner-to-multiple-activities.sql")
  @Test
  fun `returns 403 when the user role is inappropriate`() {
    val prisonerNumber = "G4793VF"
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
    val requestList = createPrisonerWaitingListRequest(1, listOf(WaitingListStatus.PENDING))

    webTestClient.post()
      .uri("/waiting-list-applications/$MOORLAND_PRISON_CODE/prisoner/$prisonerNumber")
      .bodyValue(requestList)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsUser(roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-20.sql")
  @Test
  fun `returns 404 when activity schedule id is not found`() {
    val prisonerNumber = "G4793VF"

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)

    val requestList = createPrisonerWaitingListRequest(3, listOf(WaitingListStatus.PENDING))

    webTestClient.prisonerWaitingListApplication(MOORLAND_PRISON_CODE, prisonerNumber, requestList).expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains("Activity schedule 3 not found")
      }

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()
  }

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `returns 401 for an unauthorized user when retrieving the waiting list history`() {
    val waitingListId = 1L
    webTestClient.get()
      .uri("/waiting-list-applications/$waitingListId/history")
      .accept(MediaType.APPLICATION_JSON)
      .header(CASELOAD_ID, "USERNAME")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `returns 403 for an inappropriate user role when retrieving the waiting list history`() {
    val waitingListId = 1L

    webTestClient.get()
      .uri("/waiting-list-applications/$waitingListId/history")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-20.sql")
  @Test
  fun `returns 404 when waiting list id is not found`() {
    val waitingListId = 1L

    webTestClient.waitingListApplicationForHistory(waitingListId).expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains("Waiting List 1 not found")
      }

    assertThat(waitingListRepository.findAll()).isEmpty()
    assertThat(auditRepository.findAll()).isEmpty()
  }

  @Sql("classpath:test_data/seed-activity-id-22.sql")
  @Test
  fun `returns 200 when waiting list id exists but no audit history is present`() {
    val waitingListId = 1L

    webTestClient.waitingListApplicationForHistory(waitingListId).expectStatus()
      .isOk()
      .expectBodyList(WaitingListApplicationHistory::class.java)
      .hasSize(0)
  }

  @Sql("classpath:test_data/seed-waiting-list-audit-history.sql")
  @Test
  fun `returns waiting list history ordered by latest first`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("G4793VF")

    val waitingList = listOf(
      PrisonerWaitingListApplicationRequest(
        activityScheduleId = 1L,
        applicationDate = LocalDate.of(2025, 1, 1),
        requestedBy = "Initial requester",
        comments = "Initial comment",
        status = WaitingListStatus.PENDING,
      ),
    )

    webTestClient.post()
      .uri("/waiting-list-applications/$MOORLAND_PRISON_CODE/prisoner/G4793VF")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsUser(username = "Tom", roles = listOf(ROLE_ACTIVITY_HUB, ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
      .bodyValue(waitingList)
      .exchange()
      .expectStatus().isNoContent

    val waitingListId = waitingListRepository.findAll().first().waitingListId

    // Apply first update via update endpoint
    val firstUpdate = WaitingListApplicationUpdateRequest(
      applicationDate = LocalDate.of(2025, 1, 10),
      requestedBy = "Fred",
      comments = "Fred's first revision",
      status = WaitingListStatus.APPROVED,
    )
    webTestClient.patch()
      .uri("/waiting-list-applications/$waitingListId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsUser(username = "Fred", roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
      .bodyValue(firstUpdate)
      .exchange()
      .expectStatus().isAccepted

    // Apply second update via update endpoint
    val secondUpdate = WaitingListApplicationUpdateRequest(
      applicationDate = LocalDate.of(2025, 1, 20),
      requestedBy = "Alice",
      comments = "Alice's second revision",
      status = WaitingListStatus.WITHDRAWN,
    )
    webTestClient.patch()
      .uri("/waiting-list-applications/$waitingListId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsUser(username = "Alice", roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
      .bodyValue(secondUpdate)
      .exchange()
      .expectStatus().isAccepted

    // GET the /history endpoint
    val history = webTestClient.get()
      .uri("/waiting-list-applications/$waitingListId/history")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN, ROLE_PRISON)))
      .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
      .exchange()
      .expectStatus().isOk
      .expectBodyList(WaitingListApplicationHistory::class.java)
      .returnResult()
      .responseBody!!

    // Assertions
    assertThat(history).hasSize(3)

    val original = history[2]
    val firstRev = history[1]
    val secondRev = history[0]

    assertThat(original.comments).isEqualTo("Initial comment")
    assertThat(original.status).isEqualTo(WaitingListStatus.PENDING)
    assertThat(original.requestedBy).isEqualTo("Initial requester")
    assertThat(original.applicationDate).isEqualTo(LocalDate.of(2025, 1, 1))
    assertThat(original.updatedBy).isEqualTo("Tom")

    assertThat(firstRev.comments).isEqualTo("Fred's first revision")
    assertThat(firstRev.status).isEqualTo(WaitingListStatus.APPROVED)
    assertThat(firstRev.requestedBy).isEqualTo("Fred")
    assertThat(firstRev.applicationDate).isEqualTo(LocalDate.of(2025, 1, 10))
    assertThat(firstRev.updatedBy).isEqualTo("Fred")

    assertThat(secondRev.comments).isEqualTo("Alice's second revision")
    assertThat(secondRev.status).isEqualTo(WaitingListStatus.WITHDRAWN)
    assertThat(secondRev.requestedBy).isEqualTo("Alice")
    assertThat(secondRev.applicationDate).isEqualTo(LocalDate.of(2025, 1, 20))
    assertThat(secondRev.updatedBy).isEqualTo("Alice")

    // updatedDateTime conversion to BST and GMT
    val firstRevLdnZoned = firstRev.updatedDateTime.atZone(ZoneId.of("Europe/London"))
    val secondRevLdnZoned = secondRev.updatedDateTime.atZone(ZoneId.of("Europe/London"))
    val rules = ZoneId.of("Europe/London").rules

    val offsetFirstRevSeconds = rules.getOffset(firstRevLdnZoned.toInstant()).totalSeconds
    val offsetSecondRevSeconds = rules.getOffset(secondRevLdnZoned.toInstant()).totalSeconds
    if (isBST(firstRev.updatedDateTime)) {
      assert(offsetFirstRevSeconds == 3600)
    } else {
      assert(offsetFirstRevSeconds == 0)
    }

    if (isBST(secondRev.updatedDateTime)) {
      assert(offsetSecondRevSeconds == 3600)
    } else {
      assert(offsetSecondRevSeconds == 0)
    }
    history.forEach {
      assertThat(it.id).isEqualTo(1L)
    }
  }

  private fun WebTestClient.waitingListApplication(
    prisonCode: String,
    application: WaitingListApplicationRequest,
    caseloadId: String? = CASELOAD_ID,
  ) = post()
    .uri("/allocations/$prisonCode/waiting-list-application")
    .bodyValue(application)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, caseloadId!!)
    .exchange()

  private fun WebTestClient.prisonerWaitingListApplication(
    prisonCode: String,
    prisonerNumber: String,
    request: List<PrisonerWaitingListApplicationRequest>,
    caseloadId: String? = CASELOAD_ID,
  ) = post()
    .uri("/waiting-list-applications/$prisonCode/prisoner/$prisonerNumber")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN, ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, caseloadId!!)
    .exchange()

  private fun WebTestClient.searchWaitingLists(
    prisonCode: String,
    request: WaitingListSearchRequest,
  ): LinkedHashMap<String, Any> = post().uri("/waiting-list-applications/$prisonCode/search")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, prisonCode)
    .bodyValue(request)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(typeReference<LinkedHashMap<String, Any>>())
    .returnResult().responseBody!!

  private fun WebTestClient.waitingListApplicationForHistory(
    waitingListId: Long,
  ) = get()
    .uri("/waiting-list-applications/$waitingListId/history")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN, ROLE_PRISON)))
    .header(CASELOAD_ID, MOORLAND_PRISON_CODE)
    .exchange()

  private fun isBST(localDateTime: LocalDateTime?): Boolean {
    val zone = ZoneId.of("Europe/London")
    val rules = zone.rules
    return rules.isDaylightSavings(localDateTime?.atZone(zone)?.toInstant())
  }
}
