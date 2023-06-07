package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all activities in a category for a prison`() {
    val activities = webTestClient.getActivitiesForCategory("PVI", 1)

    assertThat(activities).containsExactlyInAnyOrder(
      ActivityLite(
        id = 1,
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
        capacity = 20,
        allocated = 4,
        createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
        activityState = ActivityState.LIVE,
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all activities for a prison`() {
    val activities = webTestClient.getActivities("PVI")

    assertThat(activities).containsExactlyInAnyOrder(
      ActivityLite(
        id = 1,
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
        capacity = 20,
        allocated = 4,
        createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
        activityState = ActivityState.LIVE,
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville on Oct 10th 2022`() {
    val locations = webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10))

    assertThat(locations).containsExactlyInAnyOrder(
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
      webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.AM)

    assertThat(locations).containsExactly(InternalLocation(1, "L1", "Location 1"))
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville afternoon of Oct 10th 2022`() {
    val locations =
      webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.PM)

    assertThat(locations).containsExactly(InternalLocation(2, "L2", "Location 2"))
  }

  private fun WebTestClient.getActivitiesForCategory(prisonCode: String, categoryId: Long) =
    get()
      .uri("/prison/$prisonCode/activity-categories/$categoryId/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityLite::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getActivities(prisonCode: String) =
    get()
      .uri("/prison/$prisonCode/activities")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityLite::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getLocationsPrisonByCode(
    code: String,
    date: LocalDate? = LocalDate.now(),
    timeSlot: TimeSlot? = null,
  ) =
    get()
      .uri("/prison/$code/locations?date=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
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
        .also { assertThat(it).hasSize(2) }

    val morningSchedule = with(schedules.first()) {
      assertThat(allocations).hasSize(3)
      assertThat(instances).hasSize(1)
      this
    }

    morningSchedule.allocatedPrisoner("A11111A")
    morningSchedule.allocatedPrisoner("A22222A")

    with(morningSchedule.instances.first()) {
      assertThat(date).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(cancelled).isFalse
      assertThat(startTime).isEqualTo(LocalTime.of(10, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(11, 0))
    }

    val afternoonSchedule = with(schedules.second()) {
      assertThat(allocations).hasSize(2)
      assertThat(instances).hasSize(1)
      this
    }

    afternoonSchedule.allocatedPrisoner("A11111A")
    afternoonSchedule.allocatedPrisoner("A22222A")

    with(afternoonSchedule.instances.first()) {
      assertThat(date).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(cancelled).isFalse
      assertThat(startTime).isEqualTo(LocalTime.of(14, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(15, 0))
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get morning schedules for Pentonville prison on Monday 10th October 2022`() {
    val schedules =
      webTestClient.getSchedulesByPrison("PVI", LocalDate.of(2022, 10, 10), TimeSlot.AM)!!
        .also { assertThat(it).hasSize(1) }

    val schedule = with(schedules.first()) {
      assertThat(allocations).hasSize(3)
      assertThat(instances).hasSize(1)
      assertThat(internalLocation).isEqualTo(InternalLocation(1, "L1", "Location 1"))
      this
    }

    schedule.allocatedPrisoner("A11111A")
    schedule.allocatedPrisoner("A22222A")

    with(schedule.instances.first()) {
      assertThat(date).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(cancelled).isFalse
      assertThat(startTime).isEqualTo(LocalTime.of(10, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(11, 0))
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get afternoon schedules for Pentonville prison on Monday 10th October 2022`() {
    val schedules =
      webTestClient.getSchedulesByPrison("PVI", LocalDate.of(2022, 10, 10), TimeSlot.PM)!!
        .also { assertThat(it).hasSize(1) }

    val schedule = with(schedules.first()) {
      assertThat(allocations).hasSize(2)
      assertThat(instances).hasSize(1)
      this
    }

    schedule.allocatedPrisoner("A11111A")
    schedule.allocatedPrisoner("A22222A")

    with(schedule.instances.first()) {
      assertThat(date).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(cancelled).isFalse
      assertThat(startTime).isEqualTo(LocalTime.of(14, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(15, 0))
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
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivitySchedule::class.java)
      .returnResult().responseBody

  private fun List<ActivitySchedule>.second() = this[1]
}
