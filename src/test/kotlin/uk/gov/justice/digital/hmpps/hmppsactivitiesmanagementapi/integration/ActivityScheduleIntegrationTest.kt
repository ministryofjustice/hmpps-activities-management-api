package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.asListOfType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_DELETED
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as ModelAllAttendance

class ActivityScheduleIntegrationTest : LocalStackTestBase() {

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @Autowired
  private lateinit var activityScheduleRepository: ActivityScheduleRepository

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Autowired
  private lateinit var waitlistRepository: WaitingListRepository

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get only active allocations for Maths`() {
    val response = webTestClient.getAllocationsBy(1)!!.also { assertThat(it).hasSize(2) }
    response.forEach {
      assertThat(it.prisonerName).isNull()
      assertThat(it.prisonerFirstName).isNull()
      assertThat(it.prisonerLastName).isNull()
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

    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("PVI")

    val response = webTestClient.getAllocationsBy(1, includePrisonerSummary = true)!!

    assertThat(response)
      .extracting(Allocation::prisonerFirstName, Allocation::prisonerLastName, Allocation::cellLocation, Allocation::earliestReleaseDate, Allocation::nonAssociations)
      .containsOnly(
        tuple("Tim", "Harrison", "1-2-3", EarliestReleaseDate(LocalDate.now().plusDays(1)), false),
        tuple("Joe", "Harrison", "1-2-3", EarliestReleaseDate(LocalDate.now()), true),
      )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get only active allocations whe non-associations api returns an error`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A11111A", "A22222A"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A11111A", firstName = "Joe", releaseDate = LocalDate.now()),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A22222A", firstName = "Tim", releaseDate = LocalDate.now().plusDays(1)),
      ),
    )

    nonAssociationsApiMockServer.stubGetNonAssociationsInvolvingError()

    val response = webTestClient.getAllocationsBy(1, includePrisonerSummary = true)!!

    assertThat(response)
      .extracting(Allocation::prisonerFirstName, Allocation::prisonerLastName, Allocation::cellLocation, Allocation::earliestReleaseDate, Allocation::nonAssociations)
      .containsOnly(
        tuple("Tim", "Harrison", "1-2-3", EarliestReleaseDate(LocalDate.now().plusDays(1)), null),
        tuple("Joe", "Harrison", "1-2-3", EarliestReleaseDate(LocalDate.now()), null),
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
      .headers(setAuthorisationAsClient())
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
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  private fun WebTestClient.getAllocationsBy(
    scheduleId: Long,
    activeOnly: Boolean? = null,
    includePrisonerSummary: Boolean? = null,
    date: LocalDate? = null,
    caseLoadId: String = "PVI",
  ) = get()
    .uri { builder ->
      builder
        .path("/schedules/$scheduleId/allocations")
        .maybeQueryParam("activeOnly", activeOnly)
        .maybeQueryParam("includePrisonerSummary", includePrisonerSummary)
        .maybeQueryParam("date", date)
        .build(scheduleId)
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_PRISON)))
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
      .headers(setAuthorisationAsClient())
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
      .headers(setAuthorisationAsClient())
      .header(CASELOAD_ID, "MDI")
      .exchange()
      .expectStatus().isForbidden
  }

  private fun WebTestClient.getScheduleBy(scheduleId: Long, caseLoadId: String = "PVI", earliestSessionDate: LocalDate? = null) = get()
    .uri { builder ->
      builder
        .path("/schedules/$scheduleId")
        .maybeQueryParam("earliestSessionDate", earliestSessionDate)
        .build(scheduleId)
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ActivitySchedule::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate, eventTierType: EventTierType? = null) = get()
    .uri("/attendances/$prisonCode/$sessionDate${eventTierType?.let { "?eventTier=${it.name}" } ?: ""}")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ModelAllAttendance::class.java)
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

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    val allocation = with(activityScheduleRepository.findById(1).orElseThrow()) {
      with(allocationRepository.findByActivitySchedule(this).first()) {
        assertThat(this.prisonerNumber).isEqualTo("G4793VF")
        assertThat(this.allocatedBy).isEqualTo("test-client")
        this
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATED, allocation.allocationId),
    )

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
  @Sql("classpath:test_data/seed-activity-id-7.sql")
  fun `204 (no content) response when successfully allocate prisoner to an activity schedule that starts today (2 sessions, one is earlier, so only one attendance record should be created)`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.today(),
        scheduleInstanceId = 2L,
      ),
    ).expectStatus().isNoContent

    val allocation = with(activityScheduleRepository.findById(1).orElseThrow()) {
      with(allocationRepository.findByActivitySchedule(this).first()) {
        assertThat(this.prisonerNumber).isEqualTo("G4793VF")
        assertThat(this.allocatedBy).isEqualTo("test-client")
        this
      }
    }

    val newAttendance = attendanceRepository.findAll().filter { it.scheduledInstance.sessionDate == TimeSource.today() }
      .also { it.size isEqualTo 1 }
      .first()

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATED, allocation.allocationId),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_CREATED, newAttendance.attendanceId),
    )

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

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

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

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

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
      .headers(setAuthorisationAsUser(roles = listOf("ROLE_NOT_ALLOWED")))
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

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      val allocation = allocationRepository.findByActivitySchedule(this)

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
    caseNotesApiMockServer.stubGetCaseNote("A1143DZ", UUID.fromString("41c02efa-a46e-40ef-a2ba-73311e18e51e"))

    val response = webTestClient.getCandidateSuitability(1, "A1143DZ")
      .expectStatus().isOk
      .expectBody(typeReference<AllocationSuitability>())
      .returnResult().responseBody!!

    with(response) {
      assertThat(workplaceRiskAssessment!!.suitable).isTrue
      assertThat(workplaceRiskAssessment.riskLevel).isEqualTo("none")

      assertThat(incentiveLevel!!.suitable).isTrue
      assertThat(incentiveLevel.incentiveLevel).isEqualTo("Basic")

      assertThat(education!!.suitable).isFalse
      assertThat(education.education).isEmpty()

      assertThat(releaseDate!!.suitable).isTrue
      assertThat(releaseDate.earliestReleaseDate).isEqualTo(EarliestReleaseDate(releaseDate = LocalDate.parse("2045-04-12")))

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
    "classpath:test_data/seed-activity-for-suitability-check.sql",
  )
  fun `should be able to fetch non-associations for an allocated prisoner`() {
    nonAssociationsApiMockServer.stubGetNonAssociations("A1143DZ")

    val response = webTestClient.getNonAssociations(1, "A1143DZ")

    val expectedAllocated = NonAssociationDetails(
      allocated = true,
      reasonCode = "LEGAL_REQUEST",
      reasonDescription = "Police or legal request",
      roleCode = "PERPETRATOR",
      roleDescription = "Perpetrator",
      restrictionType = "LANDING",
      restrictionTypeDescription = "Cell and landing",
      otherPrisonerDetails = OtherPrisonerDetails(
        prisonerNumber = "A11111A",
        firstName = "YZRIRATIF",
        lastName = "AMBENTINO",
        cellLocation = "A-3-08S",
      ),
      whenUpdated = LocalDateTime.parse("2023-10-03T14:08:07"),
      comments = "Comment 1",
    )

    val expectedNonAllocated = NonAssociationDetails(
      allocated = false,
      reasonCode = "GANG_RELATED",
      reasonDescription = "Gang related",
      roleCode = "VICTIM",
      roleDescription = "Victim",
      restrictionType = "WING",
      restrictionTypeDescription = "Cell, landing and wing",
      otherPrisonerDetails = OtherPrisonerDetails(
        prisonerNumber = "G9353UC",
        firstName = "BARPRENAV",
        lastName = "TONONNE",
        cellLocation = "A-3-08S",
      ),
      whenUpdated = LocalDateTime.parse("2023-10-04T15:08:07"),
      comments = "Comment 2",
    )

    assertThat(response).containsOnly(expectedAllocated, expectedNonAllocated)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-candidates.sql",
  )
  fun `should be able to fetch a paged list of candidates for an activity`() {
    prisonerSearchApiMockServer.stubGetAllPrisonersInPrison("PVI")
    prisonApiMockServer.stubGetEducationLevels()
    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("PVI")

    webTestClient.getCandidates(1, 0, 5)
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.totalPages").isEqualTo(4)
      .jsonPath("$.totalElements").isEqualTo(20)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo("A1446DZ")
      .jsonPath("$.content[0].nonAssociations").isEqualTo(true)
      .jsonPath("$.content[1].prisonerNumber").isEqualTo("A1718DZ")
      .jsonPath("$.content[1].nonAssociations").isEqualTo(true)
      .jsonPath("$.content[2].prisonerNumber").isEqualTo("A5015DY")
      .jsonPath("$.content[2].nonAssociations").isEqualTo(false)
      .jsonPath("$.content[3].prisonerNumber").isEqualTo("A2226DZ")
      .jsonPath("$.content[3].nonAssociations").isEqualTo(false)
      .jsonPath("$.content[4].prisonerNumber").isEqualTo("A5089DY")
      .jsonPath("$.content[4].nonAssociations").isEqualTo(false)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-candidates.sql",
  )
  fun `should be able to fetch a paged list of candidates for an activity when non-associations api returns an error`() {
    prisonerSearchApiMockServer.stubGetAllPrisonersInPrison("PVI")
    prisonApiMockServer.stubGetEducationLevels()
    nonAssociationsApiMockServer.stubGetNonAssociationsInvolvingError()

    webTestClient.getCandidates(1, 0, 5)
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.totalPages").isEqualTo(4)
      .jsonPath("$.totalElements").isEqualTo(20)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo("A1446DZ")
      .jsonPath("$.content[0].nonAssociations").isEmpty
      .jsonPath("$.content[1].prisonerNumber").isEqualTo("A1718DZ")
      .jsonPath("$.content[1].nonAssociations").isEmpty
      .jsonPath("$.content[2].prisonerNumber").isEqualTo("A5015DY")
      .jsonPath("$.content[2].nonAssociations").isEmpty
      .jsonPath("$.content[3].prisonerNumber").isEqualTo("A2226DZ")
      .jsonPath("$.content[3].nonAssociations").isEmpty
      .jsonPath("$.content[4].prisonerNumber").isEqualTo("A5089DY")
      .jsonPath("$.content[4].nonAssociations").isEmpty
  }

  @Test
  @Sql(
    "classpath:test_data/seed-candidates.sql",
  )
  fun `should be able to fetch a paged list of candidates with no other allocations`() {
    prisonerSearchApiMockServer.stubGetAllPrisonersInPrison("PVI")
    prisonApiMockServer.stubGetEducationLevels()
    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("PVI")

    webTestClient.getCandidates(1, 0, 5, noAllocations = true)
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.totalPages").isEqualTo(4)
      .jsonPath("$.totalElements").isEqualTo(19)
      .jsonPath("$.content[0].prisonerNumber").isEqualTo("A1718DZ")
      .jsonPath("$.content[1].prisonerNumber").isEqualTo("A5015DY")
      .jsonPath("$.content[2].prisonerNumber").isEqualTo("A2226DZ")
      .jsonPath("$.content[3].prisonerNumber").isEqualTo("A5089DY")
      .jsonPath("$.content[4].prisonerNumber").isEqualTo("A3062DZ")
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `403 when fetching candidates for the wrong case load`() {
    webTestClient.get()
      .uri("/schedules/1/candidates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationAsClient())
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
      .headers(setAuthorisationAsClient(roles = listOf(ROLE_ACTIVITY_ADMIN)))
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

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

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

    activityScheduleRepository.findById(1).orElseThrow().also {
      with(allocationRepository.findByActivitySchedule(it).first().plannedDeallocation!!) {
        assertThat(plannedBy).isEqualTo("test-client")
        assertThat(plannedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(plannedDate).isEqualTo(TimeSource.tomorrow())
        assertThat(allocation.endDate).isEqualTo(TimeSource.tomorrow())
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-7.sql",
  )
  fun `allocation, with end date, followed by a deallocation`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
        endDate = TimeSource.tomorrow().plusDays(1),
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

    activityScheduleRepository.findById(1).orElseThrow().also {
      with(allocationRepository.findByActivitySchedule(it).first().plannedDeallocation!!) {
        assertThat(plannedBy).isEqualTo("test-client")
        assertThat(plannedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(plannedDate).isEqualTo(TimeSource.tomorrow())
        assertThat(allocation.endDate).isEqualTo(TimeSource.tomorrow())
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
    )
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

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

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

    activityScheduleRepository.findById(1).orElseThrow().also {
      with(allocationRepository.findByActivitySchedule(it).first()) {
        assertThat(deallocatedBy).isEqualTo("test-client")
        assertThat(deallocatedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(startDate).isEqualTo(TimeSource.tomorrow())
        assertThat(endDate).isEqualTo(TimeSource.tomorrow())
        assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-30.sql",
  )
  fun `deallocation before the activity schedule has started will remove advance attendances`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(
      PrisonerSearchPrisonerFixture.instance(
        prisonId = MOORLAND_PRISON_CODE,
        prisonerNumber = "G4793VF",
        bookingId = 1,
        status = "ACTIVE IN",
      ),
    )

    with(activityScheduleRepository.findById(1).orElseThrow()) {
      assertThat(allocationRepository.findByActivitySchedule(this)).isEmpty()
    }

    webTestClient.allocatePrisoner(
      1,
      PrisonerAllocationRequest(
        prisonerNumber = "G4793VF",
        payBandId = 11,
        startDate = TimeSource.tomorrow(),
      ),
    ).expectStatus().isNoContent

    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "G4793VF",
      issuePayment = true,
    )

    val attendance = webTestClient.createAdvanceAttendance(request, "MDI")

    webTestClient.retrieveAdvanceAttendance(attendance!!.id)

    webTestClient.deallocatePrisoners(
      1,
      PrisonerDeallocationRequest(
        prisonerNumbers = listOf("G4793VF"),
        reasonCode = DeallocationReason.WITHDRAWN_STAFF.name,
        endDate = TimeSource.today(),
        caseNote = null,
      ),
    ).expectStatus().isNoContent

    webTestClient.checkAdvanceAttendanceDoesNotExist(attendance.id)

    activityScheduleRepository.findById(1).orElseThrow().also {
      with(allocationRepository.findByActivitySchedule(it).first()) {
        assertThat(deallocatedBy).isEqualTo("test-client")
        assertThat(deallocatedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(startDate).isEqualTo(TimeSource.tomorrow())
        assertThat(endDate).isEqualTo(TimeSource.tomorrow())
        assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
    )
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-33.sql")
  fun `deallocate deletes attendances for later today`() {
    val scheduledInstance = webTestClient.getScheduleBy(1)!!.instances.first { i -> i.startTime == LocalTime.of(12, 0) }

    // Remove sessions starting from today PM for A11111A
    webTestClient.deallocatePrisoners(
      1,
      PrisonerDeallocationRequest(
        prisonerNumbers = listOf("A11111A"),
        reasonCode = DeallocationReason.WITHDRAWN_STAFF.name,
        endDate = TimeSource.tomorrow(),
        caseNote = null,
        scheduleInstanceId = scheduledInstance.id,
      ),
    ).expectStatus().isNoContent

    webTestClient.getAllAttendanceByDate("MDI", LocalDate.now()).also {
      // Today AM for A11111A should remain
      assertThat(it!!.filter { a -> a.scheduledInstanceId == 1L }).extracting<String> { a -> a.prisonerNumber }.containsOnly("A11111A")
      // Today PM should be empty as only A11111A
      assertThat(it.filter { a -> a.scheduledInstanceId == 2L }).isEmpty()
      // Today PM should be B22222B
      assertThat(it.filter { a -> a.scheduledInstanceId == 3L }).extracting<String> { a -> a.prisonerNumber }.containsOnly("B22222B")
    }

    webTestClient.getAllAttendanceByDate("MDI", LocalDate.now().plusDays(1)).also {
      // Tomorrow attendances should remain
      assertThat(it!!.filter { a -> a.scheduledInstanceId == 4L }).extracting<String> { a -> a.prisonerNumber }.containsOnly("A11111A")
    }

    activityScheduleRepository.findById(1).orElseThrow().also {
      val allocations = allocationRepository.findByActivitySchedule(it)

      with(allocations.first { a -> a.prisonerNumber == "A11111A" }.plannedDeallocation!!) {
        assertThat(plannedBy).isEqualTo("test-client")
        assertThat(plannedReason).isEqualTo(DeallocationReason.WITHDRAWN_STAFF)
        assertThat(plannedDate).isEqualTo(TimeSource.tomorrow())
      }

      assertThat(allocations.first { a -> a.prisonerNumber != "A11111A" }.plannedDeallocation).isNull()
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_DELETED, 10001, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_DELETED, 10001, 3),
    )
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-33.sql")
  fun `400 (bad request) response when attempt to de-allocate multiple prisoners`() {
    webTestClient.deallocatePrisoners(
      1,
      PrisonerDeallocationRequest(
        prisonerNumbers = listOf("A11111A", "B22222B)"),
        reasonCode = DeallocationReason.WITHDRAWN_STAFF.name,
        endDate = TimeSource.tomorrow(),
        caseNote = null,
        scheduleInstanceId = 123L,
      ),
    ).expectStatus().isBadRequest
  }

  @Sql(
    "classpath:test_data/seed-activity-id-21.sql",
  )
  @Test
  fun `get all waiting lists for Maths ignoring prisoners REMOVED and ALLOCATED from waitlist`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A4065DZ"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A4065DZ", firstName = "Joe", releaseDate = LocalDate.now()),
      ),
    )

    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("MDI")

    webTestClient.getWaitingListsBy(1)!!.also { assertThat(it).hasSize(1) }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-21.sql",
  )
  @Test
  fun `get all waiting lists includes non-associations details`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A4065DZ"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A4065DZ", firstName = "Joe", releaseDate = LocalDate.now()),
      ),
    )

    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("MDI")

    val result = webTestClient.getWaitingListsBy(1)

    assertThat(result).extracting<Boolean> { w -> w.nonAssociations }.containsExactly(true)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-21.sql",
  )
  @Test
  fun `get all waiting lists does not include non-associations details`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A4065DZ"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A4065DZ", firstName = "Joe", releaseDate = LocalDate.now()),
      ),
    )

    nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("MDI")

    val result = webTestClient.getWaitingListsBy(1, includeNonAssociationsCheck = false)

    assertThat(result).extracting<Boolean> { w -> w.nonAssociations }.containsExactly(null)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-21.sql",
  )
  @Test
  fun `get all waiting lists when non-associations api returns an error`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A4065DZ"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A4065DZ", firstName = "Joe", releaseDate = LocalDate.now()),
      ),
    )

    nonAssociationsApiMockServer.stubGetNonAssociationsInvolvingError()

    val result = webTestClient.getWaitingListsBy(1)

    assertThat(result).extracting<Boolean> { w -> w.nonAssociations }.containsExactly(null)
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

  private fun WebTestClient.allocatePrisoner(scheduleId: Long, request: PrisonerAllocationRequest) = post()
    .uri("/schedules/$scheduleId/allocations")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_HUB)))
    .exchange()

  private fun WebTestClient.deallocatePrisoners(scheduleId: Long, request: PrisonerDeallocationRequest) = put()
    .uri("/schedules/$scheduleId/deallocate")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()

  private fun WebTestClient.getCandidateSuitability(
    scheduleId: Long,
    prisonerNumber: String,
    caseLoadId: String = "PVI",
  ) = get()
    .uri("/schedules/$scheduleId/suitability?prisonerNumber=$prisonerNumber")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.getCandidates(
    scheduleId: Long,
    pageNum: Long = 0,
    pageSize: Long = 10,
    caseLoadId: String = "PVI",
    noAllocations: Boolean = false,
  ) = get()
    .uri("/schedules/$scheduleId/candidates?noAllocations=$noAllocations&size=$pageSize&page=$pageNum")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.getWaitingListsBy(scheduleId: Long, caseLoadId: String = MOORLAND_PRISON_CODE, includeNonAssociationsCheck: Boolean? = null) = get()
    .uri { builder ->
      builder
        .path("/schedules/$scheduleId/waiting-list-applications")
        .maybeQueryParam("includeNonAssociationsCheck", includeNonAssociationsCheck)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(WaitingListApplication::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getNonAssociations(
    scheduleId: Long,
    prisonerNumber: String,
    caseLoadId: String = "PVI",
  ) = get()
    .uri("/schedules/$scheduleId/non-associations?prisonerNumber=$prisonerNumber")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(NonAssociationDetails::class.java)
    .returnResult().responseBody
}
