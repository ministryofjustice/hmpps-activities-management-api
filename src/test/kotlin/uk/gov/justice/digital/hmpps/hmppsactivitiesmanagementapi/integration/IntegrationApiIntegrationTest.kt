package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.Hearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.between
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.attendedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.autoSuspendedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.cancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.clashReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.industriesCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.notRequiredReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.otherReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.refusedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.restReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.sickReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.suspendedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySuitabilityCriteria
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_HMPPS_INTEGRATION_API
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.prisoner.attendance-created=true",
    "feature.event.activities.prisoner.attendance-amended=true",
    "feature.event.activities.activity-schedule.created=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.event.activities.prisoner.allocated=true",
  ],
)
class IntegrationApiIntegrationTest : ActivitiesIntegrationTestBase() {

  @Nested
  inner class GetAttendances {

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `get prisoner attendance without prison code`() {
      val prisonerNumber = "A11111A"

      val attendanceList = webTestClient.getAttendanceForPrisoner(
        prisonerNumber = prisonerNumber,
        startDate = LocalDate.of(2022, 10, 10),
        endDate = LocalDate.of(2022, 10, 11),
      )

      assertThat(attendanceList.size).isEqualTo(5)
      assertThat(attendanceList.first().prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(attendanceList.first().scheduleInstanceId).isEqualTo(1)
      assertThat(attendanceList.first().attendanceReason).isNull()
      assertThat(attendanceList.first().comment).isNull()
    }

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `get prisoner attendance with prison code`() {
      val prisonerNumber = "A11111A"

      val attendanceList = webTestClient.getAttendanceForPrisoner(
        prisonCode = MOORLAND_PRISON_CODE,
        prisonerNumber = prisonerNumber,
        startDate = LocalDate.of(2022, 10, 10),
        endDate = LocalDate.of(2022, 10, 11),
      )

      assertThat(attendanceList.size).isEqualTo(5)
      assertThat(attendanceList.first().prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(attendanceList.first().scheduleInstanceId).isEqualTo(1)
      assertThat(attendanceList.first().attendanceReason).isNull()
      assertThat(attendanceList.first().comment).isNull()
    }

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `does not get prisoner attendance when no data in date range`() {
      val prisonerNumber = "A11111A"

      val attendanceList = webTestClient.getAttendanceForPrisoner(
        prisonCode = MOORLAND_PRISON_CODE,
        prisonerNumber = prisonerNumber,
        startDate = LocalDate.of(2022, 12, 10),
        endDate = LocalDate.of(2022, 12, 11),
      )

      assertThat(attendanceList.size).isEqualTo(0)
    }

    @Sql(
      "classpath:test_data/seed-attendances.sql",
    )
    @Test
    fun `get prisoner attendance with invalid prison code`() {
      val prisonerNumber = "A11111A"

      webTestClient.get()
        .uri("/integration-api/attendances/$prisonerNumber?startDate=2022-10-10&endDate=2022-10-11&prisonCode=ABC")
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `get prisoner attendance returns bad request when no dates supplied`() {
      val prisonerNumber = "A11111A"

      webTestClient.get()
        .uri("/integration-api/attendances/$prisonerNumber")
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
    }

    @Test
    fun `get prisoner attendance returns bad request when dates greater than 4 weeks apart supplied`() {
      val prisonerNumber = "A11111A"

      webTestClient.get()
        .uri("/integration-api/attendances/$prisonerNumber?startDate=2022-10-10&endDate=2022-12-11")
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
    }

    private fun WebTestClient.getAttendanceForPrisoner(
      prisonCode: String? = null,
      startDate: LocalDate,
      endDate: LocalDate,
      prisonerNumber: String,
    ) = get()
      .uri("/integration-api/attendances/$prisonerNumber?startDate=$startDate&endDate=$endDate${prisonCode?.let { "&prisonCode=$it" } ?: ""}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ModelAttendance::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("getAttendanceReasons")
  inner class GetAttendanceReasons {
    @Test
    fun `get list of attendance reasons`() {
      assertThat(webTestClient.getAttendanceReasons()!!).containsExactlyInAnyOrder(
        sickReason,
        refusedReason,
        notRequiredReason,
        restReason,
        clashReason,
        otherReason,
        suspendedReason,
        autoSuspendedReason,
        cancelledReason,
        attendedReason,
      )
    }

    private fun WebTestClient.getAttendanceReasons() = get()
      .uri("/integration-api/attendance-reasons")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AttendanceReason::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("getScheduleById")
  inner class GetScheduleById {
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
        .uri("/integration-api/schedules/1")
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
        .uri("/integration-api/schedules/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(isClientToken = false))
        .header(CASELOAD_ID, "MDI")
        .exchange()
        .expectStatus().isForbidden
    }

    private fun WebTestClient.getScheduleBy(scheduleId: Long, caseLoadId: String = "PVI", earliestSessionDate: LocalDate? = null) = get()
      .uri { builder ->
        builder
          .path("/integration-api/schedules/$scheduleId")
          .maybeQueryParam("earliestSessionDate", earliestSessionDate)
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySchedule::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("getActivitySchedules")
  inner class GetActivitySchedules {
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
            offWing = false,
            pieceWork = false,
            outsideWork = false,
            payPerSession = PayPerSession.H,
            prisonCode = "PVI",
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
            allocated = 5,
            createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
            activityState = ActivityState.LIVE,
            paid = true,
          ),
          slots = listOf(
            ActivityScheduleSlot(
              id = 1L,
              timeSlot = TimeSlot.AM,
              weekNumber = 1,
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
          scheduleWeeks = 1,
          usePrisonRegimeTime = true,
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
            allocated = 5,
            createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
            activityState = ActivityState.LIVE,
            paid = true,
          ),
          slots = listOf(
            ActivityScheduleSlot(
              id = 2L,
              timeSlot = TimeSlot.PM,
              weekNumber = 1,
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
          scheduleWeeks = 1,
          usePrisonRegimeTime = true,
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
            offWing = false,
            pieceWork = true,
            outsideWork = true,
            payPerSession = PayPerSession.H,
            prisonCode = "PVI",
            summary = "Maths",
            description = "Maths Level 1",
            riskLevel = "high",
            category = educationCategory,
            capacity = 10,
            allocated = 2,
            createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
            activityState = ActivityState.LIVE,
            paid = true,
          ),
          slots = listOf(
            ActivityScheduleSlot(
              id = 1L,
              timeSlot = TimeSlot.AM,
              weekNumber = 1,
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
              timeSlot = TimeSlot.PM,
              weekNumber = 1,
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
          scheduleWeeks = 1,
          usePrisonRegimeTime = true,
        ),
      )
    }

    @Sql(
      "classpath:test_data/seed-activity-multi-week-schedule-1.sql",
    )
    @Test
    fun `gets activity schedules for activity with multi-week schedule`() {
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
            offWing = false,
            pieceWork = true,
            outsideWork = true,
            payPerSession = PayPerSession.H,
            prisonCode = "PVI",
            summary = "Maths",
            description = "Maths Level 1",
            riskLevel = "high",
            category = educationCategory,
            capacity = 10,
            allocated = 2,
            createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
            activityState = ActivityState.LIVE,
            paid = true,
          ),
          slots = listOf(
            ActivityScheduleSlot(
              id = 1L,
              timeSlot = TimeSlot.AM,
              weekNumber = 1,
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
              timeSlot = TimeSlot.PM,
              weekNumber = 1,
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
            ActivityScheduleSlot(
              id = 3L,
              timeSlot = TimeSlot.AM,
              weekNumber = 2,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(11, 0),
              daysOfWeek = listOf("Tue", "Fri"),
              mondayFlag = false,
              tuesdayFlag = true,
              wednesdayFlag = false,
              thursdayFlag = false,
              fridayFlag = true,
              saturdayFlag = false,
              sundayFlag = false,
            ),
            ActivityScheduleSlot(
              id = 4L,
              timeSlot = TimeSlot.PM,
              weekNumber = 2,
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
          scheduleWeeks = 2,
          usePrisonRegimeTime = true,
        ),
      )
    }

    private fun WebTestClient.getSchedulesOfAnActivity(id: Long) = get()
      .uri("/activities/$id/schedules")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleLite::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("getScheduledInstancesForPrisoner")
  inner class GetScheduledInstancesForPrisoner {
    val prisonerNumber = "A11111A"

    @Test
    @Sql("classpath:test_data/seed-activity-integration-api-1.sql")
    fun `returns data in the date range with the correct prisoner number`() {
      val startDate = LocalDate.of(2022, 10, 2)
      val endDate = LocalDate.of(2022, 11, 4)

      val scheduledInstances =
        webTestClient.getScheduledInstancesForPrisonerBy(
          prisonerNumber = prisonerNumber,
          prisonCode = MOORLAND_PRISON_CODE,
          startDate = startDate,
          endDate = endDate,
        )

      assertThat(scheduledInstances).hasSize(8)
      assertThat(scheduledInstances).allMatch { it.prisonerNumber == prisonerNumber }
      assertThat(scheduledInstances).allMatch { it.prisonCode == MOORLAND_PRISON_CODE }
      assertThat(scheduledInstances).allMatch { it.sessionDate.between(startDate, endDate) }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-integration-api-1.sql")
    fun `returns data with the time slot filter`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesForPrisonerBy(
          prisonerNumber = prisonerNumber,
          prisonCode = MOORLAND_PRISON_CODE,
          startDate = startDate,
          endDate = endDate,
          timeSlot = TimeSlot.AM,
        )

      assertThat(scheduledInstances).hasSize(4)
      assertThat(scheduledInstances).allMatch { it.prisonerNumber == prisonerNumber }
      assertThat(scheduledInstances).allMatch { it.prisonCode == MOORLAND_PRISON_CODE }
      assertThat(scheduledInstances).allMatch { it.sessionDate.between(startDate, endDate) }
      assertThat(scheduledInstances).allMatch { it.timeSlot == TimeSlot.AM }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-integration-api-1.sql")
    fun `returns no data for date range outside of data`() {
      val startDate = LocalDate.now()
      val endDate = LocalDate.now().plusMonths(1)

      val scheduledInstances =
        webTestClient.getScheduledInstancesForPrisonerBy(
          prisonerNumber = prisonerNumber,
          prisonCode = MOORLAND_PRISON_CODE,
          startDate = startDate,
          endDate = endDate,
        )

      assertThat(scheduledInstances).hasSize(0)
    }
    private fun WebTestClient.getScheduledInstancesForPrisonerBy(
      prisonerNumber: String,
      prisonCode: String,
      startDate: LocalDate,
      endDate: LocalDate,
      timeSlot: TimeSlot? = null,
    ) = get()
      .uri { builder ->
        builder
          .path("/integration-api/prisons/$prisonCode/$prisonerNumber/scheduled-instances")
          .queryParam("startDate", startDate)
          .queryParam("endDate", endDate)
          .maybeQueryParam("slot", timeSlot)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(PrisonerScheduledActivity::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("getActivityScheduleSuitabilityCriteria")
  inner class GetActivityScheduleSuitabilityCriteria {
    val scheduleId = 1L

    @Test
    @Sql("classpath:test_data/seed-activity-for-suitability-check.sql")
    fun `returns suitability data for given id`() {
      val suitabilityCriteria =
        webTestClient.getActivityScheduleSuitabilityCriteria(scheduleId = scheduleId)

      assertThat(suitabilityCriteria).isNotNull
      assertThat(suitabilityCriteria?.riskLevel).isEqualTo("high")
      assertThat(suitabilityCriteria?.minimumEducationLevel).hasSize(1)
      assertThat(suitabilityCriteria?.payRates).hasSize(3)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-for-suitability-check.sql")
    fun `returns 404 for schedule id with no data `() {
      val scheduleIdWithNoData = 4L
      webTestClient.get()
        .uri { builder ->
          builder
            .path("/integration-api/activities/schedule/$scheduleIdWithNoData/suitability-criteria")
            .build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
        .exchange()
        .expectStatus().isNotFound
    }

    private fun WebTestClient.getActivityScheduleSuitabilityCriteria(
      scheduleId: Long = 1L,
    ) = get()
      .uri { builder ->
        builder
          .path("/integration-api/activities/schedule/$scheduleId/suitability-criteria")
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySuitabilityCriteria::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("/integration-api/schedules/{scheduleId}/waiting-list-applications")
  inner class GetWaitingListApplications {
    private fun WebTestClient.getWaitingListsBy(scheduleId: Long, caseLoadId: String = MOORLAND_PRISON_CODE) = get()
      .uri("/integration-api/schedules/$scheduleId/waiting-list-applications")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .header(CASELOAD_ID, caseLoadId)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(WaitingListApplication::class.java)
      .returnResult().responseBody

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

      nonAssociationsApiMockServer.stubGetNonAssociationsInvolving("MDI")

      webTestClient.getWaitingListsBy(1)!!.also { assertThat(it).hasSize(1) }
    }
  }

  @Nested
  @DisplayName("/integration-api/prison/{prisonId}/activities")
  inner class GetActivities {
    @Sql(
      "classpath:test_data/seed-activity-id-1.sql",
    )
    @Test
    fun `get all activities for a prison`() {
      val activities = webTestClient.getActivitiesIntegrationApi("PVI")!!

      activities.single() isEqualTo
        ActivitySummary(
          id = 1,
          activityName = "Maths Level 1",
          category = educationCategory,
          capacity = 10,
          allocated = 5,
          waitlisted = 1,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
        )
    }

    @Sql(
      "classpath:test_data/seed-activity-id-3.sql",
    )
    @Test
    fun `get activities for a prison when filtered by a search term`() {
      val activities = webTestClient.getActivitiesIntegrationApi("PVI", nameSearch = "enGliSh")!!

      activities.single() isEqualTo
        ActivitySummary(
          id = 4,
          activityName = "English Level 2",
          category = industriesCategory,
          capacity = 10,
          allocated = 4,
          waitlisted = 0,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
        )
    }

    fun WebTestClient.getActivitiesIntegrationApi(prisonCode: String, nameSearch: String? = null) = get()
      .uri("/integration-api/prison/$prisonCode/activities" + (nameSearch?.let { "?nameSearch=$nameSearch" } ?: ""))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_HMPPS_INTEGRATION_API)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivitySummary::class.java)
      .returnResult().responseBody
  }

  @Nested
  @DisplayName("/integration-api/scheduled-events/prison/{prisonId}")
  inner class GetScheduledEventsForSinglePrisoner {
    private fun WebTestClient.getScheduledEventsForSinglePrisoner(prisonCode: String, prisonerNumber: String, startDate: LocalDate, endDate: LocalDate) = get()
      .uri("/integration-api/scheduled-events/prison/$prisonCode?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerScheduledEvents::class.java)
      .returnResult().responseBody

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `GET single prisoner - activities active, appointments not active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A11111A"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      adjudicationsMock(prisonCode, startDate, listOf(prisonerNumber))
      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(EventType.VISIT.defaultPriority)
        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        assertThat(activities).hasSize(6)
        assertThat(activities!![0].priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
    fun `GET single prisoner - both activities and appointments active - 200 success`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A11111A"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 1)

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      adjudicationsMock(prisonCode, startDate, listOf(prisonerNumber))

      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(courtHearings).hasSize(4)
        assertThat(courtHearings!![0].priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)

        assertThat(visits).hasSize(1)
        assertThat(visits!![0].priority).isEqualTo(EventType.VISIT.defaultPriority)

        assertThat(adjudications).hasSize(1)
        assertThat(adjudications!![0].priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)

        assertThat(appointments).hasSize(1)
        appointments!!.map {
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.appointmentSeriesId).isEqualTo(3)
          assertThat(it.appointmentId).isEqualTo(4)
          assertThat(it.appointmentAttendeeId).isEqualTo(5)
          assertThat(it.internalLocationId).isEqualTo(123)
          assertThat(it.internalLocationCode).isEqualTo("No information available")
          assertThat(it.internalLocationDescription).isEqualTo("No information available")
          assertThat(it.categoryCode).isEqualTo("AC1")
          assertThat(it.categoryDescription).isEqualTo("Appointment Category 1")
          assertThat(it.summary).isEqualTo("Appointment description (Appointment Category 1)")
          assertThat(it.comments).isEqualTo("Appointment level comment")
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 10, 1))
          assertThat(it.startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(10, 30))
        }

        assertThat(activities).hasSize(6)
        assertThat(activities!![0].priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
    fun `GET single prisoner - scheduled events with exclusions are not returned`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A5193DY"
      val bookingId = 1200993L
      val startDate = LocalDate.now()
      val endDate = LocalDate.now()

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      adjudicationsMock(prisonCode, startDate, listOf(prisonerNumber))
      // No transfers - not today

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, startDate, endDate)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(LocalDate.now())
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(13, 0))
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-historical-exclusions.sql")
    fun `GET single prisoner - scheduled events with exclusions are not returned - past date`() {
      val yesterday = LocalDate.now().minusDays(1)

      val prisonCode = "MDI"
      val prisonerNumber = "A5193DY"
      val bookingId = 1200993L

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, yesterday, yesterday)
      prisonApiMockServer.stubGetCourtHearings(bookingId, yesterday, yesterday)
      adjudicationsMock(prisonCode, yesterday, listOf(prisonerNumber))

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, yesterday, yesterday)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(yesterday)
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(13, 0))
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-future-exclusions.sql")
    fun `GET single prisoner - scheduled events with exclusions are not returned - future date`() {
      val tomorrow = LocalDate.now().plusDays(1)

      val prisonCode = "MDI"
      val prisonerNumber = "A5193DY"
      val bookingId = 1200993L

      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(prisonerNumber)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, tomorrow, tomorrow)
      prisonApiMockServer.stubGetCourtHearings(bookingId, tomorrow, tomorrow)
      adjudicationsMock(prisonCode, tomorrow, listOf(prisonerNumber))

      val scheduledEvents = webTestClient.getScheduledEventsForSinglePrisoner(prisonCode, prisonerNumber, tomorrow, tomorrow)

      println(scheduledEvents)

      with(scheduledEvents!!) {
        assertThat(activities).hasSize(1)
        assertThat(activities!!.first().date).isEqualTo(tomorrow)
        assertThat(activities!!.first().startTime).isEqualTo(LocalTime.of(13, 0))
      }
    }

    @Test
    fun `GET single prisoner - neither activities nor appointments active - 404 prisoner details not found`() {
      val prisonCode = "MDI"
      val prisonerNumber = "AAAAA"
      val bookingId = 1200993L
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 10, 1)

      // Error stub - prisoner number not found
      prisonerSearchApiMockServer.stubSearchByPrisonerNumberNotFound(prisonerNumber)

      prisonApiMockServer.stubGetScheduledActivities(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledAppointments(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetScheduledVisits(bookingId, startDate, endDate)
      prisonApiMockServer.stubGetCourtHearings(bookingId, startDate, endDate)
      adjudicationsMock(prisonCode, startDate, listOf(prisonerNumber))
      // No transfers - not today

      val errorResponse = webTestClient.get()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/scheduled-events/prison/$prisonCode")
            .queryParam("prisonerNumber", prisonerNumber)
            .queryParam("startDate", startDate)
            .queryParam("endDate", endDate)
            .build(prisonerNumber)
        }
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      with(errorResponse!!) {
        assertThat(errorCode).isNull()
        assertThat(developerMessage).isEqualTo("Prisoner '$prisonerNumber' not found")
        assertThat(moreInfo).isNull()
        assertThat(status).isEqualTo(404)
        assertThat(userMessage).isEqualTo("Not found: Prisoner '$prisonerNumber' not found")
      }
    }

    private fun adjudicationsMock(
      agencyId: String,
      date: LocalDate,
      prisonerNumbers: List<String>,
    ) {
      manageAdjudicationsApiMockServer.stubHearings(
        agencyId = agencyId,
        startDate = date,
        endDate = date,
        prisoners = prisonerNumbers,
        body = mapper.writeValueAsString(
          prisonerNumbers.mapIndexed { hearingId, offenderNo ->
            HearingsResponse(
              prisonerNumber = offenderNo,
              hearing = Hearing(
                id = hearingId.plus(1).toLong(),
                oicHearingType = "GOV_ADULT",
                dateTimeOfHearing = date.atTime(10, 30, 0),
                locationId = 1L,
                agencyId = agencyId,
              ),
            )
          },
        ),
      )
    }
  }
}
