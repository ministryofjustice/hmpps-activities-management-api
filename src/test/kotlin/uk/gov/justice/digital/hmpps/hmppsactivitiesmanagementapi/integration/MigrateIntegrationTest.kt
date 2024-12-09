package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class MigrateIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  lateinit var clock: Clock

  @Autowired
  lateinit var prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository

  private val customStartTimeAM: LocalTime = LocalTime.of(9, 25, 0)
  private val regimeStartTimeAM: LocalTime = LocalTime.of(8, 25, 0)
  private val regimeEndTimeAM: LocalTime = LocalTime.of(11, 35, 0)
  private val prisonerNumber = "AE12345"
  private val now = LocalDate.now().atStartOfDay().plusMinutes(15)
  private val nextMonday = now.plusDays(7 + (DayOfWeek.MONDAY.value - now.dayOfWeek.value).toLong())

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
        NomisScheduleRule(
          startTime = LocalTime.of(6, 40, 0),
          endTime = LocalTime.of(7, 0, 0),
          saturday = true,
          sunday = true,
        ),
      ),
      payRates = emptyList(),
    )

  private val customSlotRequest =
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
          timeSlot = TimeSlot.PM,
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
        NomisScheduleRule(
          startTime = LocalTime.of(6, 40, 0),
          endTime = LocalTime.of(7, 0, 0),
          saturday = true,
          sunday = true,
        ),
      ),
      payRates = emptyList(),
    )

  private val felthamRegimeTimeRequest =
    ActivityMigrateRequest(
      programServiceCode = "INT_NOM",
      prisonCode = "FMI",
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
          startTime = LocalTime.of(8, 30, 0),
          endTime = LocalTime.of(11, 30, 0),
          monday = false,
          tuesday = false,
          wednesday = true,
          thursday = false,
          friday = true,
          sunday = true,
        ),
      ),
      payRates = emptyList(),
    )

  private val felthamCustomTimeRequest =
    ActivityMigrateRequest(
      programServiceCode = "INT_NOM",
      prisonCode = "FMI",
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
          startTime = LocalTime.of(8, 15, 0),
          endTime = LocalTime.of(11, 30, 0),
          monday = false,
          tuesday = false,
          wednesday = true,
          thursday = false,
          friday = true,
          sunday = true,
        ),
      ),
      payRates = emptyList(),
    )

  private val regimeNotFoundRequest =
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
          friday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(13, 40, 0),
          endTime = LocalTime.of(16, 50, 0),
          monday = true,
          tuesday = true,
          wednesday = true,
          thursday = true,
          friday = true,
        ),
      ),
      payRates = emptyList(),
    )

  @BeforeEach
  fun init() {
    whenever(clock.instant()).thenReturn(nextMonday.toInstant(ZoneOffset.UTC))
    whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `regime not found issue`() {
    val activityId = migrateActivity(request = regimeNotFoundRequest)
    val activity = getActivity(activityId = activityId)

    assertThat(activity.schedules.first().usePrisonRegimeTime).isFalse()
  }

  @Sql(
    "classpath:test_data/seed-fmi-prison-regime.sql",
  )
  @Test
  fun `import with regime time should set flag to true`() {
    val activityId = migrateActivity(request = felthamRegimeTimeRequest)
    val activity = getActivity(activityId = activityId, agencyId = "FMI")

    assertThat(activity.schedules.first().usePrisonRegimeTime).isTrue()
    with(activity.schedules.first().internalLocation!!) {
      assertThat(id).isEqualTo(1)
      assertThat(description).isEqualTo("House_block_7-1-002")
    }
  }

  @Sql(
    "classpath:test_data/seed-fmi-prison-regime.sql",
  )
  @Test
  fun `import location code containing 'WOW'`() {
    val activityId = migrateActivity(request = felthamRegimeTimeRequest.copy(internalLocationCode = "junkWOWkal"))
    val activity = getActivity(activityId = activityId, agencyId = "FMI")

    assertThat(activity.onWing).isTrue()
    assertThat(activity.schedules.first().internalLocation).isNull()
  }

  @Sql(
    "classpath:test_data/seed-fmi-prison-regime.sql",
  )
  @Test
  fun `import with custom time should not set flag to true`() {
    val activityId = migrateActivity(request = felthamCustomTimeRequest)
    val activity = getActivity(activityId = activityId, agencyId = "FMI")

    assertThat(activity.schedules.first().usePrisonRegimeTime).isFalse()
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
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

    val weekendAm = activity.schedules.first().slots.first {
      it.saturdayFlag && it.sundayFlag
    }

    assertThat(activity.schedules.size).isEqualTo(1)
    assertThat(mondayAm.timeSlot.name).isEqualTo(TimeSlot.AM.name)
    assertThat(mondayAm.startTime == customStartTimeAM).isTrue()
    assertThat(fridayAm.startTime == regimeStartTimeAM).isTrue()
    assertThat(weekendAm.startTime == LocalTime.of(6, 40, 0)).isTrue()
    assertThat(weekendAm.endTime == LocalTime.of(7, 0, 0)).isTrue()
    assertThat(activity.schedules.first().usePrisonRegimeTime).isFalse()
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `import activity with timeslot should set custom times and preserve the supplied slot`() {
    val activityId = migrateActivity(request = customSlotRequest)
    val activity = getActivity(activityId = activityId)

    val mondayAm = activity.schedules.first().slots.first {
      it.mondayFlag && it.endTime == regimeEndTimeAM
    }

    val fridayAm = activity.schedules.first().slots.first {
      it.fridayFlag && it.endTime == regimeEndTimeAM
    }

    val weekendAm = activity.schedules.first().slots.first {
      it.saturdayFlag && it.sundayFlag
    }

    assertThat(activity.schedules.size).isEqualTo(1)
    assertThat(mondayAm.timeSlot.name).isEqualTo(TimeSlot.PM.name)
    assertThat(mondayAm.startTime == customStartTimeAM).isTrue()
    assertThat(fridayAm.startTime == regimeStartTimeAM).isTrue()
    assertThat(weekendAm.startTime == LocalTime.of(6, 40, 0)).isTrue()
    assertThat(weekendAm.endTime == LocalTime.of(7, 0, 0)).isTrue()
    assertThat(activity.schedules.first().usePrisonRegimeTime).isFalse()
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `Edit activity slots, remove the custom slot and then reapply, observe the time has now been put back to prison regime time`() {
    val activityId = migrateActivity()

    val allSlots = listOf(
      Slot(
        weekNumber = 1,
        timeSlot = TimeSlot.AM,
        monday = true,
        tuesday = true,
        wednesday = true,
        thursday = true,
        customStartTime = customStartTimeAM,
        customEndTime = regimeEndTimeAM,
      ),
      Slot(
        weekNumber = 1,
        timeSlot = TimeSlot.PM,
        monday = true,
        tuesday = true,
        wednesday = true,
        thursday = true,
      ),
      Slot(
        weekNumber = 1,
        timeSlot = TimeSlot.PM,
        friday = true,
      ),
    )

    val slotsExcludingCustomAm = ActivityUpdateRequest(
      slots = allSlots.filter { it.friday || it.timeSlot == TimeSlot.PM },
    )

    val updated = updateActivity(activityId = activityId, slots = slotsExcludingCustomAm)

    assertThat(
      updated.schedules.first().slots.firstOrNull {
        it.mondayFlag && it.endTime == regimeEndTimeAM
      },
    ).isNull()

    val slots = ActivityUpdateRequest(
      slots = allSlots,
    )

    val updatedWithSlotBack = updateActivity(activityId = activityId, slots = slots)

    val mondayAm = updatedWithSlotBack.schedules.first().slots.first {
      it.mondayFlag && it.endTime == regimeEndTimeAM
    }

    assertThat(
      mondayAm.startTime == customStartTimeAM,
    ).isTrue()
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
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
    assertThat(activitySchedule.instances.firstOrNull { it.date == nextMonday.toLocalDate() && it.startTime == customStartTimeAM }).isNotNull
    assertThat(activitySchedule.instances.first { it.startTime == customStartTimeAM }.attendances.isNotEmpty()).isTrue()

    // move scheduler to friday
    whenever(clock.instant()).thenReturn(nextMonday.plusDays(4).toInstant(ZoneOffset.UTC))

    scheduleInstances()

    val activityScheduleFriday = getActivitySchedule(scheduleId = activity.schedules.first().id)
    assertThat(activityScheduleFriday.instances.firstOrNull { it.date == nextMonday.plusDays(4).toLocalDate() && it.startTime == regimeStartTimeAM }).isNotNull
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `set up exclusions for the custom slots, and confirm the prisoner has no attendance record`() {
    val activityId = migrateActivity()
    migrateAllocation(activityId = activityId, incExclusion = true)

    val activity = getActivity(activityId = activityId)

    assertThat(
      activity.schedules.first().allocations.first {
        it.exclusions.isNotEmpty()
      }.exclusions.first().timeSlot == TimeSlot.AM,
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
      startDate = nextMonday.toLocalDate(),
      endDate = nextMonday.toLocalDate(),
      timeSlot = null,
    )

    assertThat(data.size).isEqualTo(1)
    assertThat(data.first().startTime).isAfter(LocalTime.of(13, 0, 0))
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
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

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `create activity across multiple regimes`() {
    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-iwi.json",
    )

    val response = createActivity(
      activityCreateRequest =
      ActivityCreateRequest(
        prisonCode = "IWI",
        attendanceRequired = true,
        inCell = false,
        pieceWork = false,
        outsideWork = false,
        payPerSession = null,
        summary = "Test activity",
        description = "Test activity",
        categoryId = activityCategory().activityCategoryId,
        tierCode = eventTier().code,
        organiserCode = eventOrganiser().code,
        eligibilityRuleIds = emptyList(),
        pay = emptyList(),
        riskLevel = "high",
        startDate = TimeSource.tomorrow(),
        endDate = null,
        minimumEducationLevel = emptyList(),
        locationId = 1,
        capacity = 1,
        scheduleWeeks = 1,
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = TimeSlot.AM,
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
          ),
        ),
        onWing = false,
        offWing = false,
        paid = false,
      ),
    )

    assertThat(response.schedules.first().usePrisonRegimeTime).isTrue()
    assertThat(response.schedules.first().slots.size).isEqualTo(3)
    assertThat(response.schedules.first().slots.none { it.sundayFlag }).isTrue()
    assertThat(response.schedules.first().slots.any { it.saturdayFlag }).isTrue()

    // now update it to be mon - sat
    val updated = updateActivity(
      activityId = response.id,
      slots =
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = TimeSlot.AM,
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
          ),
        ),
      ),
    )

    assertThat(updated.schedules.first().usePrisonRegimeTime).isTrue()
    assertThat(updated.schedules.first().slots.size).isEqualTo(3)

    val now = LocalTime.of(9, 0, 0)
    // finally update it to use custom times.
    val updatedToCustom = updateActivity(
      activityId = response.id,
      slots =
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = TimeSlot.AM,
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
            customStartTime = now,
            customEndTime = now.plusHours(1),
          ),
        ),
      ),
    )

    assertThat(updatedToCustom.schedules.first().usePrisonRegimeTime).isFalse()
    assertThat(updatedToCustom.schedules.first().slots.size).isEqualTo(1)
    assertThat(updatedToCustom.schedules.first().slots.first().startTime).isEqualTo(now)
    assertThat(updatedToCustom.schedules.first().slots.first().endTime).isEqualTo(now.plusHours(1))
  }

  private fun migrateActivity(request: ActivityMigrateRequest = exceptionRequest): Long {
    incentivesApiMockServer.stubGetIncentiveLevels(request.prisonCode)
    prisonApiMockServer.stubGetLocation(468492L, "prisonapi/location-id-1.json")

    return webTestClient.post()
      .uri("/migrate/activity")
      .bodyValue(request)
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
          Slot(weekNumber = 1, timeSlot = TimeSlot.AM, monday = true),
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

  private fun getActivity(activityId: Long, agencyId: String = "IWI"): Activity =
    webTestClient.get()
      .uri("/activities/$activityId/filtered")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, agencyId)
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

  private fun createActivity(
    activityCreateRequest: ActivityCreateRequest,
  ): Activity =
    webTestClient.post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, "IWI")
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Activity::class.java)
      .returnResult().responseBody!!
}
