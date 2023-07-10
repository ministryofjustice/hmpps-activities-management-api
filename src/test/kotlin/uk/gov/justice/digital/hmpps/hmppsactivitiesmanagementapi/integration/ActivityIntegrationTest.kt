package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testActivityPayRateBand1
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testActivityPayRateBand2
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testActivityPayRateBand3
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandThree
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.BankHolidayService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ScheduleCreatedInformation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.activities.activity-schedule.created=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
  ],
)
class ActivityIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var bankHolidayService: BankHolidayService

  @MockBean
  private lateinit var hmppsAuditApiClient: HmppsAuditApiClient

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @Test
  @Sql("classpath:test_data/clear-local-audit.sql")
  fun `createActivity - is successful`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    val createActivityRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json").copy(startDate = TimeSource.tomorrow())

    val activity = webTestClient.createActivity(createActivityRequest)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(1)
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(pay.size).isEqualTo(2)
      assertThat(createdBy).isEqualTo("test-client")
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_CREATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"IT level 1\",\"prisonCode\":\"MDI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("An activity called 'IT level 1'(1) with category Education and starting on ${TimeSource.tomorrow()} at prison MDI was created")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `createActivity - failed duplicate prison code - summary`() {
    val activityCreateRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-2.json").copy(startDate = TimeSource.tomorrow())

    val error = webTestClient.post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Exception: Duplicate activity name detected for this prison (PVI): 'Maths'")
      assertThat(developerMessage).isEqualTo("Duplicate activity name detected for this prison (PVI): 'Maths'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `createActivity - failed authorisation`() {
    val activityCreateRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-2.json").copy(startDate = TimeSource.tomorrow())

    val error = webTestClient.post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
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

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all schedules of an activity`() {
    val schedules = webTestClient.getSchedulesOfAnActivity(1)

    assertThat(schedules).containsExactlyInAnyOrder(
      ActivityScheduleLite(
        id = 1,
        description = "Maths AM",
        internalLocation = InternalLocation(1, "L1", "Location 1"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          prisonCode = "PVI",
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          minimumIncentiveNomisCode = "BAS",
          minimumIncentiveLevel = "Basic",
          minimumEducationLevel = listOf(
            ActivityMinimumEducationLevel(
              id = 1,
              educationLevelCode = "1",
              educationLevelDescription = "Reading Measure 1.0",
              studyAreaCode = "ENGLA",
              studyAreaDescription = "English Language",
            ),
          ),
          category = educationCategory,
          capacity = 20,
          allocated = 4,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            daysOfWeek = listOf("Mon"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
      ),
      ActivityScheduleLite(
        id = 2,
        description = "Maths PM",
        internalLocation = InternalLocation(2, "L2", "Location 2"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          prisonCode = "PVI",
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          minimumIncentiveNomisCode = "BAS",
          minimumIncentiveLevel = "Basic",
          minimumEducationLevel = listOf(
            ActivityMinimumEducationLevel(
              id = 1,
              educationLevelCode = "1",
              educationLevelDescription = "Reading Measure 1.0",
              studyAreaCode = "ENGLA",
              studyAreaDescription = "English Language",
            ),
          ),
          category = educationCategory,
          capacity = 20,
          allocated = 4,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 2L,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 0),
            daysOfWeek = listOf("Mon"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-8.sql",
  )
  @Test
  fun `get schedules of an activity with multiple slots`() {
    val schedules = webTestClient.getSchedulesOfAnActivity(1)

    assertThat(schedules).containsExactly(
      ActivityScheduleLite(
        id = 1,
        description = "Maths AM",
        internalLocation = InternalLocation(1, "L1", "Location 1"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          pieceWork = true,
          outsideWork = true,
          payPerSession = PayPerSession.H,
          prisonCode = "PVI",
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          minimumIncentiveNomisCode = "BAS",
          minimumIncentiveLevel = "Basic",
          category = educationCategory,
          capacity = 10,
          allocated = 2,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            daysOfWeek = listOf("Mon", "Wed"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = true,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
          ActivityScheduleSlot(
            id = 2L,
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 0),
            daysOfWeek = listOf("Mon", "Thu"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = true,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get scheduled maths activities with morning and afternoon`() {
    val mathsLevelOneActivity = with(webTestClient.getActivityById(1)) {
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(attendanceRequired).isTrue
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(category).isEqualTo(educationCategory)
      assertThat(tier).isEqualTo(ActivityTier(1, "T1", "Tier 1"))
      assertThat(pay).isEqualTo(listOf(testActivityPayRateBand1, testActivityPayRateBand2, testActivityPayRateBand3))
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(schedules).hasSize(2)
      this
    }

    val mathsMorning = with(mathsLevelOneActivity.schedule("Maths AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(3)
      assertThat(internalLocation?.id).isEqualTo(1)
      assertThat(internalLocation?.code).isEqualTo("L1")
      assertThat(internalLocation?.description).isEqualTo("Location 1")
      this
    }

    with(mathsMorning.allocatedPrisoner("A11111A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandOne)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    with(mathsMorning.allocatedPrisoner("A22222A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandTwo)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    val mathsAfternoon = with(mathsLevelOneActivity.schedule("Maths PM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(2)
      assertThat(internalLocation?.code).isEqualTo("L2")
      assertThat(internalLocation?.description).isEqualTo("Location 2")
      this
    }

    with(mathsAfternoon.allocatedPrisoner("A11111A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 10, 0))
    }

    with(mathsAfternoon.allocatedPrisoner("A22222A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 10, 0))
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-2.sql",
  )
  @Test
  fun `get scheduled english activities for morning and afternoon`() {
    val englishLevelTwoActivity = with(webTestClient.getActivityById(2)) {
      assertThat(attendanceRequired).isTrue
      assertThat(summary).isEqualTo("English")
      assertThat(description).isEqualTo("English Level 2")
      assertThat(category).isEqualTo(educationCategory)
      assertThat(tier).isEqualTo(ActivityTier(2, "T2", "Tier 2"))
      assertThat(pay).isEqualTo(listOf(testActivityPayRateBand1, testActivityPayRateBand2, testActivityPayRateBand3))
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(schedules).hasSize(2)
      this
    }

    val englishMorning = with(englishLevelTwoActivity.schedule("English AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(3)
      assertThat(internalLocation?.code).isEqualTo("L3")
      assertThat(internalLocation?.description).isEqualTo("Location 3")
      this
    }

    with(englishMorning.allocatedPrisoner("B11111B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandOne)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishMorning.allocatedPrisoner("B22222B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandTwo)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    val englishAfternoon = with(englishLevelTwoActivity.schedule("English PM")) {
      assertThat(description).isEqualTo("English PM")
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(4)
      assertThat(internalLocation?.code).isEqualTo("L4")
      assertThat(internalLocation?.description).isEqualTo("Location 4")
      this
    }

    with(englishAfternoon.allocatedPrisoner("B11111B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishAfternoon.allocatedPrisoner("B22222B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }
  }

  private fun WebTestClient.getSchedulesOfAnActivity(id: Long) =
    get()
      .uri("/activities/$id/schedules")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleLite::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getActivityById(id: Long) =
    get()
      .uri("/activities/$id")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.createActivity(
    activityCreateRequest: ActivityCreateRequest,
  ) =
    post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody

  private fun WebTestClient.updateActivity(
    prisonCode: String,
    id: Long,
    activityUpdateRequest: ActivityUpdateRequest,
  ) =
    patch()
      .uri("/activities/$prisonCode/activityId/$id")
      .bodyValue(activityUpdateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody!!

  private fun Activity.schedule(description: String) = schedules.schedule(description)

  private fun List<ActivitySchedule>.schedule(description: String) =
    firstOrNull { it.description.uppercase() == description.uppercase() }
      ?: throw RuntimeException("Activity schedule $description not found.")

  @Test
  @Sql("classpath:test_data/clear-local-audit.sql")
  fun `the activity should be persisted even if the subsequent event notification fails`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    val createActivityRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json").copy(startDate = TimeSource.tomorrow())

    prisonApiMockServer.stubGetLocation(
      locationId = 1,
      location = Location(
        locationId = 1,
        locationType = "CELL",
        description = "House_block_7-1-002",
        agencyId = "MDI",
        currentOccupancy = 1,
        locationPrefix = "LEI-House-block-7-1-002",
        operationalCapacity = 2,
        userDescription = "user description",
        internalLocationCode = "internal location code",
      ),
    )

    whenever(eventsPublisher.send(any())).thenThrow(RuntimeException("Publishing failure"))
    val activity = webTestClient.createActivity(createActivityRequest)!!

    with(activity) {
      assertThat(summary).isEqualTo("IT level 1")
      assertThat(startDate).isEqualTo(TimeSource.tomorrow())
      assertThat(description).isEqualTo("A basic IT course")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity - is successful`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-PVI.json",
    )

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activity = webTestClient.updateActivity(pentonvillePrisonCode, 1, updateActivityRequest)

    with(activity) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(summary).isEqualTo("IT level 1 - updated")
      assertThat(tier!!.id).isEqualTo(1)
      assertThat(pay.size).isEqualTo(1)
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(schedules.first().updatedBy).isEqualTo("test-client")
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_UPDATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"IT level 1 - updated\",\"prisonCode\":\"PVI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_UPDATED)
      assertThat(prisonCode).isEqualTo(pentonvillePrisonCode)
      assertThat(message).startsWith("An activity called 'IT level 1 - updated'(1) with category Education and starting on 2022-10-10 at prison PVI was updated")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-update-slot.sql")
  fun `updateActivity slot and instances - is successful`() {
    with(webTestClient.getActivityById(1).schedules.first()) {
      assertThat(slots).hasSize(1)
      assertThat(slots.first().daysOfWeek).containsExactly("Mon")
      assertThat(instances).isEmpty()
    }

    val mondayTuesdaySlot = ActivityUpdateRequest(slots = listOf(Slot("AM", monday = true, tuesday = true)))

    with(webTestClient.updateActivity(pentonvillePrisonCode, 1, mondayTuesdaySlot).schedules.first()) {
      assertThat(slots).hasSize(1)
      assertThat(slots.first().daysOfWeek).containsExactly("Mon", "Tue")
      assertThat(instances).hasSize(4)
    }

    val thursdayFridaySlot = ActivityUpdateRequest(slots = listOf(Slot("AM", thursday = true)))

    with(webTestClient.updateActivity(pentonvillePrisonCode, 1, thursdayFridaySlot).schedules.first()) {
      assertThat(slots).hasSize(1)
      assertThat(slots.first().daysOfWeek).containsExactly("Thu")
      assertThat(instances).hasSize(2)
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }

    with(eventCaptor.secondValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-update-end-date.sql")
  fun `updateActivity end date - is successful`() {
    with(webTestClient.getActivityById(1)) {
      assertThat(endDate).isNull()
      assertThat(schedules).hasSize(1)
      assertThat(schedules.first().endDate).isNull()
      assertThat(schedules.first().allocations).hasSize(1)
      assertThat(schedules.first().allocations.first().endDate).isNull()
    }

    val newEndDate = ActivityUpdateRequest(endDate = TimeSource.tomorrow())

    with(webTestClient.updateActivity(pentonvillePrisonCode, 1, newEndDate)) {
      assertThat(endDate).isEqualTo(TimeSource.tomorrow())
      assertThat(schedules.first().endDate).isEqualTo(TimeSource.tomorrow())
      assertThat(schedules.first().allocations.first().endDate).isEqualTo(TimeSource.tomorrow())
    }
  }

  @Test
  fun `updateActivity - runs on bank holidays updated to not run on bankholidays`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    val startDate = TimeSource.tomorrow()

    whenever(bankHolidayService.isEnglishBankHoliday(startDate)) doReturn true

    val createActivityRequest: ActivityCreateRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json")
        .copy(
          startDate = startDate,
          slots = listOf(
            Slot(
              timeSlot = "AM",
              monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
              tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
              wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
              thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
              friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
              saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
              sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
            ),
          ),
          runsOnBankHoliday = true,
        )

    val activity = webTestClient.createActivity(createActivityRequest)!!

    activity.schedules.flatMap { it.instances } hasSize 2

    val updatedActivity = webTestClient.updateActivity(activity.prisonCode, activity.id, ActivityUpdateRequest(runsOnBankHoliday = false))

    updatedActivity.schedules.flatMap { it.instances } hasSize 1
  }

  @Test
  fun `updateActivity - does not run on bank holidays changed to does run on bank holidays`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    val startDate = TimeSource.tomorrow()

    whenever(bankHolidayService.isEnglishBankHoliday(startDate)) doReturn true

    val createActivityRequest: ActivityCreateRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json")
        .copy(
          startDate = startDate,
          slots = listOf(
            Slot(
              timeSlot = "AM",
              monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
              tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
              wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
              thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
              friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
              saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
              sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
            ),
          ),
          runsOnBankHoliday = false,
        )

    val activity = webTestClient.createActivity(createActivityRequest)!!

    activity.schedules.flatMap { it.instances } hasSize 1

    val updatedActivity = webTestClient.updateActivity(activity.prisonCode, activity.id, ActivityUpdateRequest(runsOnBankHoliday = true))

    updatedActivity.schedules.flatMap { it.instances } hasSize 2
  }
}
