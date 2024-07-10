package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.asListOfType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAllocatedInformation
import java.time.LocalDate
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

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()
  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @Autowired
  private lateinit var repository: ActivityScheduleRepository

  @Autowired
  private lateinit var waitlistRepository: WaitingListRepository

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get only active allocations for Maths`() {
    val response = webTestClient.getAllocationsBy(1)!!.also { assertThat(it).hasSize(2) }
    response.forEach {
      assertThat(it.prisonerName).isNull()
      assertThat(it.cellLocation).isNull()
      assertThat(it.earliestReleaseDate).isNull()
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get only active allocations for Maths with prisoner information`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A11111A", "A22222A"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A11111A", firstName = "Joe", releaseDate = LocalDate.now()),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A22222A", firstName = "Tim", releaseDate = LocalDate.now().plusDays(1)),
      ),
    )

    val response = webTestClient.getAllocationsBy(1, includePrisonerSummary = true)!!

    // response will be in random order
    assertThat(response)
      .extracting(Allocation::prisonerName, Allocation::cellLocation, Allocation::earliestReleaseDate)
      .containsOnly(
        tuple("Tim Harrison", "1-2-3", EarliestReleaseDate(LocalDate.now().plusDays(1))),
        tuple("Joe Harrison", "1-2-3", EarliestReleaseDate(LocalDate.now())),
      )
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
  fun `get all allocations for activity schedule on date`() {
    webTestClient.getAllocationsBy(1, date = LocalDate.parse("2023-10-10"))!!
      .also { assertThat(it).hasSize(2) }
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
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  private fun WebTestClient.getAllocationsBy(
    scheduleId: Long,
    activeOnly: Boolean? = null,
    includePrisonerSummary: Boolean? = null,
    date: LocalDate? = null,
    caseLoadId: String = "PVI",
  ) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId/allocations")
          .maybeQueryParam("activeOnly", activeOnly)
          .maybeQueryParam("includePrisonerSummary", includePrisonerSummary)
          .maybeQueryParam("date", date)
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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

  private fun WebTestClient.getScheduleBy(scheduleId: Long, caseLoadId: String = "PVI", earliestSessionDate: LocalDate? = null) =
    get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId")
          .maybeQueryParam("earliestSessionDate", earliestSessionDate)
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySchedule::class.java)
      .returnResult().responseBody

  @Test
  @Sql("classpath:test_data/seed-activity-id-7.sql")
  fun `204 (no content) response when successfully allocate prisoner to an activity schedule`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

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
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

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
    prisonApiMockServer.stubGetPrisonerDetails("G4793VF")

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
  fun `allocation should set any APPROVED waitlist applications to ALLOCATED status`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    with(repository.findById(1).orElseThrow()) {
      val allocation = allocations().first()

      with(waitlistRepository.findByActivitySchedule(this).first()) {
        assertThat(status).isEqualTo(WaitingListStatus.ALLOCATED)
        assertThat(allocation).isEqualTo(allocation)
      }
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-for-suitability-check.sql",
  )
  fun `should be able to fetch suitability of a candidate for an activity`() {
    prisonApiMockServer.stubGetEducationLevels()
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("A1143DZ")
    nonAssociationsApiMockServer.stubGetNonAssociations("A1143DZ")
    caseNotesApiMockServer.stubGetCaseNote("A1143DZ", 1)

    val response = webTestClient.getCandidateSuitability(1, "A1143DZ")
      .expectStatus().isOk
      .expectBody(typeReference<AllocationSuitability>())
      .returnResult().responseBody!!

    with(response) {
      assertThat(workplaceRiskAssessment!!.suitable).isTrue
      assertThat(workplaceRiskAssessment!!.riskLevel).isEqualTo("none")

      assertThat(incentiveLevel!!.suitable).isTrue
      assertThat(incentiveLevel!!.incentiveLevel).isEqualTo("Basic")

      assertThat(education!!.suitable).isFalse
      assertThat(education!!.education).isEmpty()

      assertThat(releaseDate!!.suitable).isTrue
      assertThat(releaseDate!!.earliestReleaseDate).isEqualTo(EarliestReleaseDate(releaseDate = LocalDate.parse("2045-04-12")))

      assertThat(nonAssociation!!.suitable).isFalse
      assertThat(nonAssociation!!.nonAssociations).isEqualTo(
        listOf(
          NonAssociationDetails(
            reasonCode = PrisonerNonAssociation.Reason.LEGAL_REQUEST.toString(),
            reasonDescription = "Police or legal request",
            otherPrisonerDetails = OtherPrisonerDetails(
              prisonerNumber = "A11111A",
              firstName = "YZRIRATIF",
              lastName = "AMBENTINO",
              cellLocation = "A-3-08S",
            ),
            whenCreated = LocalDateTime.parse("2023-10-03T14:08:07"),
            comments = "",
          ),
        ),
      )

      assertThat(allocations.size).isEqualTo(1)
      with(allocations.first()) {
        assertThat(payRate!!.rate).isEqualTo(325)
        assertThat(allocation.activitySummary).isEqualTo("Maths")
      }

      assertThat(previousDeallocations.size).isEqualTo(1)
      with(previousDeallocations.first()) {
        assertThat(allocation.deallocatedReason?.code).isEqualTo("SECURITY")
        assertThat(caseNoteText).isEqualTo("Case Note Text")
      }
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-candidates.sql",
  )
  fun `should be able to fetch a paged list of candidates for an activity`() {
    prisonerSearchApiMockServer.stubGetAllPrisonersInPrison("PVI")
    prisonApiMockServer.stubGetEducationLevels()

    val response = webTestClient.getCandidates(1, 0, 5)
      .expectStatus().isOk
      .expectBody(typeReference<LinkedHashMap<String, Any>>())
      .returnResult().responseBody!!

    assertThat((response["content"] as List<*>).asListOfType<ActivityCandidate>()).hasSize(5)
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
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
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

    assertThat((response["content"] as List<*>).asListOfType<ActivityCandidate>()).isEmpty()
    assertThat(response["totalPages"]).isEqualTo(4)
    assertThat(response["totalElements"]).isEqualTo(20)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
  )
  fun `allocation followed by a deallocation of the same prisoner`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

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
        caseNote = null,
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

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-30.sql",
  )
  fun `allocation followed by a deallocation of the same prisoner before the activity schedule has started`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

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
        endDate = TimeSource.today(),
        caseNote = null,
      ),
    ).expectStatus().isNoContent

    repository.findById(1).orElseThrow().also {
      with(it.allocations().first()) {
        assertThat(deallocatedBy).isEqualTo("test-client")
        assertThat(deallocatedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(startDate).isEqualTo(TimeSource.tomorrow())
        assertThat(endDate).isEqualTo(TimeSource.tomorrow())
        assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
      }
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo("activities.prisoner.allocated")
    assertThat(eventCaptor.secondValue.eventType).isEqualTo("activities.prisoner.allocation-amended")
  }

  @Sql(
    "classpath:test_data/seed-activity-id-21.sql",
  )
  @Test
  fun `get all waiting lists for Maths ignoring prisoners REMOVED from waitlist`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A4065DZ"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A4065DZ", firstName = "Joe", releaseDate = LocalDate.now()),
      ),
    )

    webTestClient.getWaitingListsBy(1)!!.also { assertThat(it).hasSize(1) }
  }

  @Sql(
    "classpath:test_data/seed-reduced-activity-instances.sql",
  )
  @Test
  fun `should get all 7 recent instances for a given schedule`() {
    webTestClient.getScheduleBy(1)!!.instances hasSize 7
  }

  @Sql(
    "classpath:test_data/seed-reduced-activity-instances.sql",
  )
  @Test
  fun `should get reduced number instances for a given schedule`() {
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = TimeSource.today())!!.instances hasSize 1
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 1.daysAgo())!!.instances hasSize 2
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 2.daysAgo())!!.instances hasSize 3
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 3.daysAgo())!!.instances hasSize 4
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 4.daysAgo())!!.instances hasSize 5
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 5.daysAgo())!!.instances hasSize 6
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 6.daysAgo())!!.instances hasSize 7
    webTestClient.getScheduleBy(scheduleId = 1, earliestSessionDate = 7.daysAgo())!!.instances hasSize 7
  }

  private fun WebTestClient.allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest) =
    post()
      .uri("/schedules/$scheduleId/allocations")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .exchange()

  private fun WebTestClient.deallocatePrisoners(scheduleId: Long, request: PrisonerDeallocationRequest) =
    put()
      .uri("/schedules/$scheduleId/deallocate")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()

  private fun WebTestClient.getCandidateSuitability(
    scheduleId: Long,
    prisonerNumber: String,
    caseLoadId: String = "PVI",
  ) =
    get()
      .uri("/schedules/$scheduleId/suitability?prisonerNumber=$prisonerNumber")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.getCandidates(
    scheduleId: Long,
    pageNum: Long = 0,
    pageSize: Long = 10,
    caseLoadId: String = "PVI",
  ) =
    get()
      .uri("/schedules/$scheduleId/candidates?size=$pageSize&page=$pageNum")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.getWaitingListsBy(scheduleId: Long, caseLoadId: String = MOORLAND_PRISON_CODE) =
    get()
      .uri("/schedules/$scheduleId/waiting-list-applications")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(WaitingListApplication::class.java)
      .returnResult().responseBody
}
