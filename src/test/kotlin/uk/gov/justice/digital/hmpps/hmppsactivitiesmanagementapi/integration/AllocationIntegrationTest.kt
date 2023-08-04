package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.startsWith
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import java.time.LocalDate
import java.time.LocalDateTime

@TestPropertySource(
  properties = [
    "feature.audit.service.local.enabled=true",
    "feature.audit.service.hmpps.enabled=true",
  ],
)
class AllocationIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @MockBean
  private lateinit var hmppsAuditApiClient: HmppsAuditApiClient

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `get allocation by id`() {
    with(webTestClient.getAllocationBy(1)!!) {
      assertThat(prisonerNumber).isEqualTo("A11111A")
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandOne)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    with(webTestClient.getAllocationBy(2)!!) {
      assertThat(prisonerNumber).isEqualTo("A22222A")
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandTwo)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `403 when attempting to get an allocation with the wrong case load ID`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false))
      .header(CASELOAD_ID, "XXX")
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `attempting to get an allocation without specifying a caseload succeeds if admin role present`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `attempting to get an allocation without specifying a caseload succeeds if the token is a client token`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = true))
      .exchange()
      .expectStatus().isOk
  }

  @Sql("classpath:test_data/seed-activity-id-20.sql")
  @Test
  fun `add prisoner to a waiting list for an activity`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", fullInfo = true)

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

    webTestClient.waitingListApplication(moorlandPrisonCode, request, moorlandPrisonCode).expectStatus().isNoContent

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
      auditType isEqualTo AuditType.PRISONER
      detailType isEqualTo AuditEventType.PRISONER_ADDED_TO_WAITING_LIST
      activityId isEqualTo 1L
      prisonCode isEqualTo moorlandPrisonCode
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
  fun `attempting to add waiting list to activity from a different caseload returns a 403`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "G4793VF",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Some comments from Bob",
      status = WaitingListStatus.PENDING,
    )

    webTestClient.waitingListApplication(moorlandPrisonCode, request, pentonvillePrisonCode).expectStatus().isForbidden
  }

  private fun WebTestClient.waitingListApplication(
    prisonCode: String,
    application: WaitingListApplicationRequest,
    caseloadId: String? = CASELOAD_ID,
  ) =
    post()
      .uri("/allocations/$prisonCode/waiting-list-application")
      .bodyValue(application)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf("ROLE_ACTIVITY_HUB")))
      .header(CASELOAD_ID, caseloadId)
      .exchange()

  private fun WebTestClient.getAllocationBy(allocationId: Long) =
    get()
      .uri("/allocations/id/$allocationId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Allocation::class.java)
      .returnResult().responseBody
}
