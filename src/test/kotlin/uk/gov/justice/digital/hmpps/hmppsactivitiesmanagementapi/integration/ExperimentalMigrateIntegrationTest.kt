package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@ActiveProfiles("experimental")
class ExperimentalMigrateIntegrationTest : IntegrationTestBase() {

  @MockBean
  lateinit var clock: Clock

  @Autowired
  lateinit var prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository

  private val customStartTimeAM: LocalTime = LocalTime.of(9, 25, 0)
  private val regimeStartTimeAM: LocalTime = LocalTime.of(8, 25, 0)
  private val regimeEndTimeAM: LocalTime = LocalTime.of(11, 35, 0)
  private val prisonerNumber = "AE12345"

  private val exceptionRequest =
    ActivityMigrateRequest(
      programServiceCode = "INT_NOM",
      prisonCode = "IWI",
      startDate = LocalDate.of(2024, 7, 9),
      endDate = null,
      internalLocationId = 468492,
      internalLocationCode = "SITE 3",
      internalLocationDescription = "IWI-ESTAB-SITE 3",
      capacity = 1,
      description = "BNM + 27 PK",
      payPerSession = "H",
      runsOnBankHoliday = true,
      outsideWork = false,
      scheduleRules = listOf(
        NomisScheduleRule(
          startTime = customStartTimeAM,
          endTime = regimeEndTimeAM,
          monday = true,
          tuesday = true,
          wednesday = true,
          thursday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(13, 40, 0),
          endTime = LocalTime.of(16, 50, 0),
          monday = true,
          tuesday = true,
          wednesday = true,
          thursday = true,
        ),
        NomisScheduleRule(
          startTime = regimeStartTimeAM,
          endTime = regimeEndTimeAM,
          friday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(13, 40, 0),
          endTime = LocalTime.of(16, 0, 0),
          friday = true,
        ),
      ),
      payRates = emptyList(),
    )

  @BeforeEach
  fun init() {
    val now = LocalDateTime.now()

    val nextMonday = LocalDateTime.now().plusDays(7 + (DayOfWeek.MONDAY.value - now.dayOfWeek.value).toLong())

    whenever(clock.instant()).thenReturn(
      Instant.parse("2024-08-05T00:15:00Z"),
    )

    whenever(clock.zone).thenReturn(ZoneId.of("UCT"))
  }

  @Sql(
    "classpath:test_data/seed-migrate-experiment.sql",
  )
  @Test
  fun `import activity should set custom times in slot`() {
    val activityId = migrateActivity()
    val activity = getActivity(activityId = activityId)

    val mondayAm = activity.schedules.first().slots.first {
      it.mondayFlag && it.endTime == regimeEndTimeAM
    }

    val fridayAm = activity.schedules.first().slots.first {
      it.fridayFlag && it.endTime == regimeEndTimeAM
    }

    assertThat(mondayAm.timeSlot.name).isEqualTo(TimeSlot.AM.name)
    assertThat(mondayAm.usePrisonRegimeTime).isFalse()
    assertThat(mondayAm.startTime == customStartTimeAM).isTrue()
    assertThat(fridayAm.usePrisonRegimeTime).isTrue()
    assertThat(fridayAm.startTime == regimeStartTimeAM).isTrue()
  }

  @Sql(
    "classpath:test_data/seed-migrate-experiment.sql",
  )
  @Test
  fun `Edit activity slots, remove the custom slot and then reapply, observe the time has now been put back to prison regime time `() {
    val activityId = migrateActivity()

    val allSlots = listOf(
      Slot(
        weekNumber = 1,
        timeSlot = "AM",
        monday = true,
        tuesday = true,
        wednesday = true,
        thursday = true,
      ),
      Slot(
        weekNumber = 1,
        timeSlot = "PM",
        monday = true,
        tuesday = true,
        wednesday = true,
        thursday = true,
      ),
      Slot(
        weekNumber = 1,
        timeSlot = "PM",
        friday = true,
      ),
    )

    val slotsExcludingCustomAm = ActivityUpdateRequest(
      slots = allSlots.filter { it.friday || it.timeSlot == "PM" },
    )

    val updated = updateActivity(activityId = activityId, slots = slotsExcludingCustomAm)

    assertThat(
      updated.schedules.first().slots.none {
        !it.fridayFlag && it.startTime == customStartTimeAM
      },
    ).isTrue()

    val slots = ActivityUpdateRequest(
      slots = allSlots,
    )

    val updatedWithSlotBack = updateActivity(activityId = activityId, slots = slots)

    assertThat(
      updatedWithSlotBack.schedules.first().slots.any {
        !it.fridayFlag && it.startTime == regimeStartTimeAM
      },
    ).isTrue()

    assertThat(
      updatedWithSlotBack.schedules.first().slots.any {
        !it.fridayFlag && it.startTime == customStartTimeAM
      },
    ).isTrue()
  }

  @Sql(
    "classpath:test_data/seed-migrate-experiment.sql",
  )
  @Test
  fun `migrate allocation and add attendance and confirm prisoner has allocation and attendance in custom slot`() {
    val activityId = migrateActivity()
    migrateAllocation(activityId = activityId)

    val activity = getActivity(activityId = activityId)

    assertThat(
      activity.schedules.first().allocations.any {
        it.prisonerNumber == prisonerNumber
      },
    )

    scheduleInstances()
    manageAttendance()

    val activitySchedule = getActivitySchedule(scheduleId = activity.schedules.first().id)

    assertThat(activitySchedule.instances.isNotEmpty()).isTrue()
    assertThat(activitySchedule.instances.size).isEqualTo(4)
    assertThat(activitySchedule.instances.first { it.startTime == customStartTimeAM }.attendances.isNotEmpty()).isTrue()
  }

  @Sql(
    "classpath:test_data/seed-migrate-experiment.sql",
  )
  @Test
  fun `set up exclusions for the custom slots, and confirm the prisoner has no attendance record`() {
    val activityId = migrateActivity()
    migrateAllocation(activityId = activityId, incExclusion = true)

    val activity = getActivity(activityId = activityId)

    assertThat(
      activity.schedules.first().allocations.first {
        it.exclusions.isNotEmpty()
      }.exclusions.first().timeSlot == "AM",
    ).isTrue()

    assertThat(
      activity.schedules.first().allocations.first {
        it.exclusions.isNotEmpty()
      }.exclusions.first().monday,
    ).isTrue()

    scheduleInstances()
    manageAttendance()

    val activitySchedule = getActivitySchedule(scheduleId = activity.schedules.first().id)
    assertThat(activitySchedule.instances.first { it.startTime == customStartTimeAM }.attendances.isEmpty()).isTrue()
    val data = prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
      prisonCode = "IWI",
      prisonerNumber = prisonerNumber,
      startDate = LocalDate.of(2024, 8, 5),
      endDate = LocalDate.of(2024, 8, 5),
    )

    assertThat(data.size).isEqualTo(1)
    assertThat(data.first().startTime).isAfter(LocalTime.of(13, 0, 0))
  }

  @Sql(
    "classpath:test_data/seed-migrate-experiment.sql",
  )
  @Test
  fun `exclude prisoner on migrate, then remove custom exclusion, and confirm prisoner has attendance`() {
    val activityId = migrateActivity()
    migrateAllocation(activityId = activityId, incExclusion = true)
    val activity = getActivity(activityId = activityId)

    updateAllocation(allocationId = activity.schedules.first().allocations.first().id)

    scheduleInstances()
    manageAttendance()

    val activitySchedule = getActivitySchedule(scheduleId = activity.schedules.first().id)
    assertThat(activitySchedule.instances.first { it.startTime == customStartTimeAM }.attendances.isNotEmpty()).isTrue()
  }

  private fun migrateActivity(): Long {
    incentivesApiMockServer.stubGetIncentiveLevels("IWI")
    prisonApiMockServer.stubGetLocation(468492L, "prisonapi/location-id-1.json")

    return webTestClient.post()
      .uri("/migrate/activity")
      .bodyValue(exceptionRequest)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivityMigrateResponse::class.java)
      .returnResult().responseBody!!.activityId
  }

  private fun migrateAllocation(activityId: Long, incExclusion: Boolean = false) {
    val request = AllocationMigrateRequest(
      prisonCode = "IWI",
      activityId = activityId,
      splitRegimeActivityId = null,
      prisonerNumber = prisonerNumber,
      bookingId = 1,
      cellLocation = "MDI-1-1-001",
      nomisPayBand = "1",
      startDate = LocalDate.now().minusDays(1),
      endDate = null,
      endComment = null,
      suspendedFlag = false,
      exclusions = if (incExclusion) {
        listOf(
          Slot(weekNumber = 1, timeSlot = "AM", monday = true),
        )
      } else {
        emptyList()
      },
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf(prisonerNumber),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = prisonerNumber,
          bookingId = 1,
          prisonId = "IWI",
          status = "ACTIVE IN",
        ),
      ),
    )
    webTestClient.post()
      .uri("/migrate/allocation")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
  }

  private fun scheduleInstances() {
    webTestClient.post()
      .uri("/job/create-scheduled-instances")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated

    Thread.sleep(1000)
  }

  private fun manageAttendance() {
    webTestClient.post()
      .uri("/job/manage-attendance-records")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated

    Thread.sleep(1000)
  }

  private fun updateAllocation(allocationId: Long) {
    webTestClient.patch()
      .uri("/allocations/IWI/allocationId/$allocationId")
      .bodyValue(
        AllocationUpdateRequest(
          exclusions = emptyList(),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, "IWI")
      .exchange()
      .expectStatus().isAccepted
  }

  private fun getActivitySchedule(scheduleId: Long): ActivitySchedule =
    webTestClient.get()
      .uri { builder ->
        builder
          .path("/schedules/$scheduleId")
          .build(scheduleId)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, "IWI")
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivitySchedule::class.java)
      .returnResult().responseBody!!

  private fun getActivity(activityId: Long): Activity =
    webTestClient.get()
      .uri("/activities/$activityId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, "IWI")
      .exchange()
      .expectBody(Activity::class.java)
      .returnResult().responseBody!!

  private fun updateActivity(activityId: Long, slots: ActivityUpdateRequest): Activity =
    webTestClient.patch()
      .uri("/activities/IWI/activityId/$activityId")
      .bodyValue(slots)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, "IWI")
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody!!
}
