package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactly
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all activities in a category for a prison`() {
    val activities = webTestClient.getActivitiesForCategory("PVI", 1)!!

    activities.single() isEqualTo
      ActivityLite(
        id = 1,
        prisonCode = "PVI",
        attendanceRequired = true,
        inCell = false,
        onWing = false,
        offWing = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = PayPerSession.H,
        summary = "Maths",
        description = "Maths Level 1",
        riskLevel = "high",
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
        paid = true,
      )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all activities for a prison`() {
    val activities = webTestClient.getActivities("PVI")!!

    activities.single() isEqualTo
      ActivitySummary(
        id = 1,
        activityName = "Maths Level 1",
        category = educationCategory,
        capacity = 10,
        allocated = 4,
        waitlisted = 1,
        createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
        activityState = ActivityState.LIVE,
      )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville on Oct 10th 2022`() {
    val locations = webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10))!!

    locations containsExactlyInAnyOrder listOf(
      InternalLocation(1, "L1", "Location 1"),
      InternalLocation(2, "L2", "Location 2"),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville morning of Oct 10th 2022`() {
    val locations =
      webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.AM)!!

    locations.single() isEqualTo InternalLocation(1, "L1", "Location 1")
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville afternoon of Oct 10th 2022`() {
    val locations =
      webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.PM)!!

    locations.single() isEqualTo InternalLocation(2, "L2", "Location 2")
  }

  private fun WebTestClient.getActivitiesForCategory(prisonCode: String, categoryId: Long) =
    get()
      .uri("/prison/$prisonCode/activity-categories/$categoryId/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityLite::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getActivities(prisonCode: String) =
    get()
      .uri("/prison/$prisonCode/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivitySummary::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getLocationsPrisonByCode(
    code: String,
    date: LocalDate? = LocalDate.now(),
    timeSlot: TimeSlot? = null,
  ) =
    get()
      .uri("/prison/$code/locations?date=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(InternalLocation::class.java)
      .returnResult().responseBody

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all schedules for Pentonville prison on Monday 10th October 2022`() {
    val schedules =
      webTestClient.getSchedulesByPrison("PVI", LocalDate.of(2022, 10, 10))!!
        .also { it hasSize 2 }

    val morningSchedule = with(schedules.single { it.description == "Maths AM" }) {
      allocations.map(Allocation::prisonerNumber) containsExactly listOf("A11111A", "A22222A", "A33333A")
      instances hasSize 1
      this
    }

    with(morningSchedule.instances.single()) {
      date isEqualTo LocalDate.of(2022, 10, 10)
      cancelled isBool false
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
    }

    val afternoonSchedule = with(schedules.single { it.description == "Maths PM" }) {
      allocations.map(Allocation::prisonerNumber) containsExactly listOf("A11111A", "A22222A")
      instances hasSize 1
      this
    }

    with(afternoonSchedule.instances.single()) {
      date isEqualTo LocalDate.of(2022, 10, 10)
      cancelled isBool false
      startTime isEqualTo LocalTime.of(14, 0)
      endTime isEqualTo LocalTime.of(15, 0)
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get morning schedules for Pentonville prison on Monday 10th October 2022`() {
    val schedules =
      webTestClient.getSchedulesByPrison("PVI", LocalDate.of(2022, 10, 10), TimeSlot.AM)!!

    val schedule = with(schedules.single { it.description == "Maths AM" }) {
      allocations.map(Allocation::prisonerNumber) containsExactly listOf("A11111A", "A22222A", "A33333A")
      instances hasSize 1
      internalLocation isEqualTo InternalLocation(1, "L1", "Location 1")
      this
    }

    with(schedule.instances.single()) {
      date isEqualTo LocalDate.of(2022, 10, 10)
      cancelled isBool false
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get afternoon schedules for Pentonville prison on Monday 10th October 2022`() {
    val schedules =
      webTestClient.getSchedulesByPrison("PVI", LocalDate.of(2022, 10, 10), TimeSlot.PM)!!

    val schedule = with(schedules.single { it.description == "Maths PM" }) {
      allocations.map(Allocation::prisonerNumber) containsExactly listOf("A11111A", "A22222A")
      instances hasSize 1
      this
    }

    with(schedule.instances.single()) {
      date isEqualTo LocalDate.of(2022, 10, 10)
      cancelled isBool false
      startTime isEqualTo LocalTime.of(14, 0)
      endTime isEqualTo LocalTime.of(15, 0)
    }
  }

  private fun WebTestClient.getSchedulesByPrison(
    prisonCode: String,
    date: LocalDate? = LocalDate.now(),
    timeSlot: TimeSlot? = null,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/prison/$prisonCode/schedules")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivitySchedule::class.java)
      .returnResult().responseBody
}
