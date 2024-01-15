package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAllocatedInformation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@TestPropertySource(
  properties = [
    "feature.event.activities.prisoner.allocation-amended=true",
  ],
)
class AllocationIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

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

  @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
  @Test
  fun `get allocation - active exclusions are returned`() {
    with(webTestClient.getAllocationBy(2)!!) {
      exclusions hasSize 1
    }

    with(webTestClient.getAllocationBy(3)!!) {
      exclusions hasSize 0
    }
  }

  @Sql("classpath:test_data/seed-activity-with-future-exclusions.sql")
  @Test
  fun `get allocation - future exclusions are returned`() {
    with(webTestClient.getAllocationBy(2)!!) {
      exclusions hasSize 1
    }
  }

  @Sql("classpath:test_data/seed-activity-with-historical-exclusions.sql")
  @Test
  fun `get allocation - past exclusions are not returned`() {
    with(webTestClient.getAllocationBy(2)!!) {
      exclusions hasSize 0
    }
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `403 when attempting to get an allocation with the wrong case load ID`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
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
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `attempting to get an allocation without specifying a caseload succeeds if the token is a client token`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
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

    webTestClient.waitingListApplication(MOORLAND_PRISON_CODE, request, PENTONVILLE_PRISON_CODE).expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `update allocation end date`() {
    allocationRepository.findById(1).get().also { it.endDate isEqualTo null }

    webTestClient.updateAllocation(
      PENTONVILLE_PRISON_CODE,
      1,
      AllocationUpdateRequest(
        endDate = TimeSource.tomorrow(),
        reasonCode = "OTHER",
      ),
    )

    allocationRepository.findById(1).get().also { it.endDate isEqualTo TimeSource.tomorrow() }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      eventType isEqualTo "activities.prisoner.allocation-amended"
      additionalInformation isEqualTo PrisonerAllocatedInformation(1)
      occurredAt isCloseTo TimeSource.now()
    }
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `update allocation exclusions`() {
    webTestClient.updateAllocation(
      PENTONVILLE_PRISON_CODE,
      1,
      AllocationUpdateRequest(
        exclusions = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "AM",
            monday = true,
          ),
        ),
      ),
    )

    val allocation = webTestClient.getAllocationBy(1)!!

    with(allocation.exclusions) {
      this hasSize 1
      this.first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      eventType isEqualTo "activities.prisoner.allocation-amended"
      additionalInformation isEqualTo PrisonerAllocatedInformation(1)
      occurredAt isCloseTo TimeSource.now()
    }
  }

  private fun WebTestClient.updateAllocation(
    prisonCode: String,
    allocationId: Long,
    application: AllocationUpdateRequest,
    caseloadId: String? = CASELOAD_ID,
  ) =
    patch()
      .uri("/allocations/$prisonCode/allocationId/$allocationId")
      .bodyValue(application)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, caseloadId)
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Allocation::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.waitingListApplication(
    prisonCode: String,
    application: WaitingListApplicationRequest,
    caseloadId: String? = CASELOAD_ID,
  ) =
    post()
      .uri("/allocations/$prisonCode/waiting-list-application")
      .bodyValue(application)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, caseloadId)
      .exchange()

  private fun WebTestClient.getAllocationBy(allocationId: Long) =
    get()
      .uri("/allocations/id/$allocationId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Allocation::class.java)
      .returnResult().responseBody
}
