package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAllocatedInformation
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.activities.prisoner.allocated=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
    "feature.event.activities.prisoner.allocation-amended=true",
  ],
)
class ActivityScheduleIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var hmppsAuditApiClient: HmppsAuditApiClient

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()
  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @Autowired
  private lateinit var repository: ActivityScheduleRepository

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get only active allocations for Maths`() {
    webTestClient.getAllocationsBy(1)!!
      .also { assertThat(it).hasSize(2) }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all allocations for Maths`() {
    webTestClient.getAllocationsBy(1, false)!!
      .also { assertThat(it).hasSize(3) }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `403 when fetching allocations for the wrong case load`() {
    webTestClient.get()
      .uri("/schedules/1/allocations")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false))
      .header(CASELOAD_ID, "MDI")
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `attempting to fetch allocations without specifying a caseload succeeds if admin role present`() {
    webTestClient.get()
      .uri("/schedules/1/allocations")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  private fun WebTestClient.getAllocationsBy(
    scheduleId: Long,
    activeOnly: Boolean? = null,
    caseLoadId: String = "PVI",
  ) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId/allocations")
          .maybeQueryParam("activeOnly", activeOnly)
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Allocation::class.java)
      .returnResult().responseBody

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get schedules by their ids`() {
    with(webTestClient.getScheduleBy(1)!!) {
      assertThat(id).isEqualTo(1)
    }

    with(webTestClient.getScheduleBy(2)!!) {
      assertThat(id).isEqualTo(2)
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `403 when fetching schedule for the wrong case load`() {
    webTestClient.get()
      .uri("/schedules/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false))
      .header(CASELOAD_ID, "MDI")
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `attempting to get a schedule without specifying a caseload succeeds if admin role present`() {
    webTestClient.get()
      .uri("/schedules/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false))
      .header(CASELOAD_ID, "MDI")
      .exchange()
      .expectStatus().isForbidden
  }

  private fun WebTestClient.getScheduleBy(scheduleId: Long, caseLoadId: String = "PVI") =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId")
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySchedule::class.java)
      .returnResult().responseBody

  @Test
  @Sql("classpath:test_data/seed-activity-id-7.sql")
  fun `204 (no content) response when successfully allocate prisoner to an activity schedule`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", fullInfo = false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    val allocation = with(repository.findById(1).orElseThrow().allocations().first()) {
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(allocatedBy).isEqualTo("test-client")
      this
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocated")
      assertThat(additionalInformation).isEqualTo(PrisonerAllocatedInformation(allocation.allocationId))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner has been allocated to an activity in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      println(details)
      assertThat(what).isEqualTo("PRISONER_ALLOCATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"Maths\",\"prisonCode\":\"MDI\",\"prisonerNumber\":\"G4793VF\",\"scheduleId\":1,\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.PRISONER)
      assertThat(detailType).isEqualTo(AuditEventType.PRISONER_ALLOCATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("Prisoner G4793VF was allocated to activity 'Maths'(1) and schedule Maths AM(1)")
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
  )
  fun `400 (bad request) response when attempt to allocate already allocated prisoner`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", fullInfo = false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
      ),
    ).expectStatus().isBadRequest
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
  )
  fun `403 (forbidden) response when user doesnt have correct role to allocate prisoner`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", fullInfo = false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    val error = webTestClient.post()
      .uri("/schedules/1/allocations")
      .bodyValue(
        PrisonerAllocationRequest(
          prisonerNumber = "G4793VF",
          payBandId = 11,
          startDate = TimeSource.tomorrow(),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOT_ALLOWED")))
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
  )
  fun `the allocation should be persisted even if the subsequent event notification fails`() {
    whenever(eventsPublisher.send(any())).thenThrow(RuntimeException("Publishing failure"))

    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", fullInfo = false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    with(repository.findById(1).orElseThrow().allocations().first()) {
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(allocatedBy).isEqualTo("test-client")
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `should be able to fetch a paged list of candidates for an activity`() {
    prisonerSearchApiMockServer.stubGetAllPrisonersInPrison("PVI")
    prisonApiMockServer.stubGetEducationLevels()

    val response = webTestClient.getCandidates(1, 2, 5)
      .expectStatus().isOk
      .expectBody(typeReference<LinkedHashMap<String, Any>>())
      .returnResult().responseBody!!

    assertThat(response["content"] as List<ActivityCandidate>).hasSize(5)
    assertThat(response["totalPages"]).isEqualTo(4)
    assertThat(response["totalElements"]).isEqualTo(20)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `403 when fetching candidates for the wrong case load`() {
    webTestClient.get()
      .uri("/schedules/1/candidates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false))
      .header(CASELOAD_ID, "MDI")
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `attempting to fetch candidates without specifying a caseload succeeds if admin role present`() {
    webTestClient.get()
      .uri("/schedules/1/candidates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `should handle candidate pagination where page param is more than the number of pages available`() {
    prisonerSearchApiMockServer.stubGetAllPrisonersInPrison("PVI")
    prisonApiMockServer.stubGetEducationLevels()

    val response = webTestClient.getCandidates(1, 20, 5)
      .expectStatus().isOk
      .expectBody(typeReference<LinkedHashMap<String, Any>>())
      .returnResult().responseBody!!

    assertThat(response["content"] as List<ActivityCandidate>).isEmpty()
    assertThat(response["totalPages"]).isEqualTo(4)
    assertThat(response["totalElements"]).isEqualTo(20)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
  )
  fun `allocation followed by a deallocation of the same prisoner`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", fullInfo = false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    webTestClient.deallocatePrisoners(
      1,
      PrisonerDeallocationRequest(
        prisonerNumbers = listOf("G4793VF"),
        reasonCode = DeallocationReason.WITHDRAWN_STAFF.name,
        endDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    repository.findById(1).orElseThrow().also {
      with(it.allocations().first().plannedDeallocation!!) {
        assertThat(plannedBy).isEqualTo("test-client")
        assertThat(plannedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(plannedDate).isEqualTo(TimeSource.tomorrow())
      }
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo("activities.prisoner.allocated")
    assertThat(eventCaptor.secondValue.eventType).isEqualTo("activities.prisoner.allocation-amended")
  }

  private fun WebTestClient.allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest) =
    post()
      .uri("/schedules/$scheduleId/allocations")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ACTIVITY_ADMIN)))
      .exchange()

  private fun WebTestClient.deallocatePrisoners(scheduleId: Long, request: PrisonerDeallocationRequest) =
    put()
      .uri("/schedules/$scheduleId/deallocate")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ACTIVITY_ADMIN)))
      .exchange()

  private fun WebTestClient.getCandidates(
    scheduleId: Long,
    pageNum: Long = 0,
    pageSize: Long = 10,
    caseLoadId: String = "PVI",
  ) =
    get()
      .uri("/schedules/$scheduleId/candidates?size=$pageSize&page=$pageNum")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  @Sql(
    "classpath:test_data/seed-activity-id-21.sql",
  )
  @Test
  fun `get all waiting lists for Maths`() {
    webTestClient.getWaitingListsBy(1)!!.also { assertThat(it).hasSize(1) }
  }

  private fun WebTestClient.getWaitingListsBy(scheduleId: Long, caseLoadId: String = moorlandPrisonCode) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId/waiting-list-applications")
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf("ROLE_ACTIVITY_HUB")))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(WaitingListApplication::class.java)
      .returnResult().responseBody
}
