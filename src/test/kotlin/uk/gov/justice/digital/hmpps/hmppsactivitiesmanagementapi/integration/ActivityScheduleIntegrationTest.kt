package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerAllocatedInformation
import java.time.LocalDateTime

class ActivityScheduleIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: EventsPublisher
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Autowired
  private lateinit var repository: ActivityScheduleRepository

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get only active allocations for Maths`() {
    webTestClient.getAllocationsBy(1)!!
      .also { assertThat(it).hasSize(2) }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
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
    "classpath:test_data/seed-activity-id-1.sql"
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
    "classpath:test_data/seed-activity-id-7.sql"
  )
  fun `204 (no content) response when successfully allocate prisoner to an activity schedule`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
      )
    ).expectStatus().isNoContent

    var allocationId: Long

    with(repository.findById(1).orElseThrow()) {
      allocationId = allocations().first().allocationId
      assertThat(allocations().first().prisonerNumber).isEqualTo("G4793VF")
      assertThat(allocations().first().allocatedBy).isEqualTo("test-client")
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocated")
      assertThat(additionalInformation).isEqualTo(PrisonerAllocatedInformation(allocationId))
      assertThat(occurredAt).isEqualToIgnoringSeconds(LocalDateTime.now())
      assertThat(description).isEqualTo("A prisoner has been allocated to an activity in the activities management service")
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql"
  )
  fun `400 (bad request) response when attempt to allocate already allocated prisoner`() {
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF", false)

    repository.findById(1).orElseThrow().also { assertThat(it.allocations()).isEmpty() }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
      )
    ).expectStatus().isNoContent

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
      )
    ).expectStatus().isBadRequest
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql"
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

  private fun WebTestClient.allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest) =
    post()
      .uri("/schedules/$scheduleId/allocations")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
}
