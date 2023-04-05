package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerAllocatedInformation
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.activities.prisoner.allocated=true",
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
  ],
)
class ActivityScheduleIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: EventsPublisher

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
  fun `get all active allocations for Maths`() {
    webTestClient.getAllocationsBy(1, false)!!
      .also { assertThat(it).hasSize(3) }
  }

  private fun WebTestClient.getAllocationsBy(scheduleId: Long, activeOnly: Boolean? = null) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId/allocations")
          .maybeQueryParam("activeOnly", activeOnly)
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
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

  private fun WebTestClient.getScheduleBy(scheduleId: Long) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId")
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySchedule::class.java)
      .returnResult().responseBody

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
    "classpath:test_data/clear-local-audit.sql",
  )
  fun `204 (no content) response when successfully allocate prisoner to an activity schedule`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
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
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
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
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    val error = webTestClient.post()
      .uri("/schedules/1/allocations")
      .bodyValue(
        PrisonerAllocationRequest(
          prisonerNumber = "G4793VF",
          payBandId = 11,
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

    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
      ),
    ).expectStatus().isNoContent

    with(repository.findById(1).orElseThrow().allocations().first()) {
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(allocatedBy).isEqualTo("test-client")
    }
  }

  private fun WebTestClient.allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest) =
    post()
      .uri("/schedules/$scheduleId/allocations")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
}
