package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityScheduleCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduleCreatedInformation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ActivityIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: EventsPublisher
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Autowired
  private lateinit var scheduledInstanceRepository: ScheduledInstanceRepository

  @Test
  fun `createActivity - is successful`() {
    prisonApiMockServer.stubGetEducationLevel("EDU_LEVEL", "1", "prisonapi/education-level-code-1.json")

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-1.json"),
      object : TypeReference<ActivityCreateRequest>() {},
    )

    val activity = webTestClient.createActivity(createActivityRequest)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(1)
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(pay.size).isEqualTo(2)
      assertThat(createdBy).isEqualTo("test-client")
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `createActivity - failed duplicate prison code - summary`() {
    val activityCreateRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-2.json"),
      object : TypeReference<ActivityCreateRequest>() {},
    )

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
    val activityCreateRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-2.json"),
      object : TypeReference<ActivityCreateRequest>() {},
    )

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
            ),
          ),
          category = educationCategory,
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
            ),
          ),
          category = educationCategory,
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
          inCell = true,
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
    val mathsLevelOneActivity = with(webTestClient.getActivityById(1)!!) {
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(attendanceRequired).isTrue
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(category).isEqualTo(educationCategory)
      assertThat(tier).isEqualTo(ActivityTier(1, "T1", "Tier 1"))
      assertThat(pay).hasSize(1)
      pay.map {
        assertThat(it.incentiveNomisCode).isEqualTo("BAS")
        assertThat(it.incentiveLevel).isEqualTo("Basic")
        assertThat(it.prisonPayBand).isEqualTo(testPentonvillePayBandOne)
        assertThat(it.rate).isEqualTo(125)
        assertThat(it.pieceRate).isEqualTo(150)
        assertThat(it.pieceRateItems).isEqualTo(1)
      }
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
    val englishLevelTwoActivity = with(webTestClient.getActivityById(2)!!) {
      assertThat(attendanceRequired).isTrue
      assertThat(summary).isEqualTo("English")
      assertThat(description).isEqualTo("English Level 2")
      assertThat(category).isEqualTo(educationCategory)
      assertThat(tier).isEqualTo(ActivityTier(2, "T2", "Tier 2"))
      pay.map {
        assertThat(it.incentiveNomisCode).isEqualTo("BAS")
        assertThat(it.incentiveLevel).isEqualTo("Basic")
        assertThat(it.prisonPayBand).isEqualTo(testPentonvillePayBandOne)
        assertThat(it.rate).isEqualTo(75)
        assertThat(it.pieceRate).isEqualTo(0)
        assertThat(it.pieceRateItems).isEqualTo(0)
      }
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
      .returnResult().responseBody

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

  private fun Activity.schedule(description: String) = schedules.schedule(description)

  private fun List<ActivitySchedule>.schedule(description: String) =
    firstOrNull { it.description.uppercase() == description.uppercase() }
      ?: throw RuntimeException("Activity schedule $description not found.")

  @Sql(
    "classpath:test_data/seed-activity-id-9.sql",
  )
  @Test
  fun `schedule an activity`() {
    val today = LocalDate.now()
    val activityScheduleCreateRequest = ActivityScheduleCreateRequest(
      description = "Integration test schedule",
      startDate = today,
      locationId = 1,
      capacity = 10,
      slots = listOf(Slot("AM", monday = true)),
    )

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

    val schedule = webTestClient.createActivitySchedule(9, activityScheduleCreateRequest)!!

    with(schedule) {
      assertThat(capacity).isEqualTo(10)
      assertThat(startDate).isEqualTo(today)
      assertThat(description).isEqualTo("Integration test schedule")
      assertThat(slots).hasSize(1)
      assertThat(internalLocation).isEqualTo(
        InternalLocation(
          id = 1,
          code = "internal location code",
          description = "House_block_7-1-002",
        ),
      )
    }

    with(schedule.slots.first()) {
      assertThat(id).isNotNull
      assertThat(daysOfWeek).containsExactly("Mon")
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(12, 0))
    }

    val scheduleInstances = scheduledInstanceRepository.getActivityScheduleInstancesByActivityScheduleId(schedule.id)
    assertThat(scheduleInstances.size).isEqualTo(1)
    assertThat(scheduleInstances.first().scheduledInstanceId).isNotNull
    assertThat(scheduleInstances.first().startTime).isEqualTo(LocalTime.of(9, 0))
    assertThat(scheduleInstances.first().endTime).isEqualTo(LocalTime.of(12, 0))

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isEqualToIgnoringSeconds(LocalDateTime.now())
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-9.sql",
  )
  @Test
  fun `the activity should be persisted even if the subsequent event notification fails`() {
    val today = LocalDate.now()
    val activityScheduleCreateRequest = ActivityScheduleCreateRequest(
      description = "Integration test schedule",
      startDate = today,
      locationId = 1,
      capacity = 10,
      slots = listOf(Slot("AM", monday = true)),
    )

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
    val schedule = webTestClient.createActivitySchedule(9, activityScheduleCreateRequest)!!

    with(schedule) {
      assertThat(capacity).isEqualTo(10)
      assertThat(startDate).isEqualTo(today)
      assertThat(description).isEqualTo("Integration test schedule")
      assertThat(slots).hasSize(1)
      assertThat(internalLocation).isEqualTo(
        InternalLocation(
          id = 1,
          code = "internal location code",
          description = "House_block_7-1-002",
        ),
      )
    }

    with(schedule.slots.first()) {
      assertThat(id).isNotNull
      assertThat(daysOfWeek).containsExactly("Mon")
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(12, 0))
    }
  }

  private fun WebTestClient.createActivitySchedule(
    activityId: Long,
    activityScheduleCreateRequest: ActivityScheduleCreateRequest,
  ) =
    post()
      .uri("/activities/$activityId/schedules")
      .bodyValue(activityScheduleCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivityScheduleLite::class.java)
      .returnResult().responseBody
}
