package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.api.IncentivesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ExclusionsFilter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonIncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class MigrateActivityServiceTest {
  private val rolloutPrisonService: RolloutPrisonService = mock()
  private val activityRepository: ActivityRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val incentivesApiClient: IncentivesApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val eventOrganiserRepository: EventOrganiserRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()

  private val listOfCategories = listOf(
    ActivityCategory(1, "SAA_EDUCATION", "Education", "desc"),
    ActivityCategory(2, "SAA_INDUSTRIES", "Industries", "desc"),
    ActivityCategory(3, "SAA_PRISON_JOBS", "Prison jobs", "desc"),
    ActivityCategory(4, "SAA_GYM_SPORTS_FITNESS", "Gym, sports, fitness", "desc"),
    ActivityCategory(5, "SAA_INDUCTION", "Induction", "desc"),
    ActivityCategory(6, "SAA_INTERVENTIONS", "Interventions", "desc"),
    ActivityCategory(7, "SAA_FAITH_SPIRITUALITY", "Faith", "desc"),
    ActivityCategory(8, "SAA_NOT_IN_WORK", "Not in work", "desc"),
    ActivityCategory(9, "SAA_OTHER", "Other", "desc"),
  )

  private val listOfTiers = listOf(
    EventTier(1, "TIER_1", "Tier 1"),
    EventTier(2, "TIER_2", "Tier 2"),
    EventTier(3, "FOUNDATION", "Foundation"),
  )

  private val eventOrganiserOther = EventOrganiser(1L, "OTHER", "Someone else")

  private val rolledOutMoorland = rolledOutPrison("MDI")
  private val rolledOutRisley = rolledOutPrison("RSI")
  private val payBandsMoorland = prisonPayBands("MDI")
  private val payBandsRisley = prisonPayBands("RSI")

  private fun prisonPayBands(prisonCode: String) = listOf(
    PrisonPayBand(1L, prisonCode, 1, "1", "Pay band 1", 1),
    PrisonPayBand(2L, prisonCode, 2, "2", "Pay band 2", 2),
    PrisonPayBand(3L, prisonCode, 3, "3", "Pay band 3", 3),
  )

  private val service = MigrateActivityService(
    false,
    rolloutPrisonService,
    activityRepository,
    prisonRegimeService,
    activityScheduleRepository,
    prisonerSearchApiClient,
    incentivesApiClient,
    prisonApiClient,
    eventTierRepository,
    activityCategoryRepository,
    prisonPayBandRepository,
    eventOrganiserRepository,
    TransactionHandler(),
    outboundEventsService,
  )

  private fun getCategory(code: String): ActivityCategory? = listOfCategories.find { it.code == code }

  private fun getTier(code: String): EventTier? = listOfTiers.find { it.code == code }

  private fun rolledOutPrison(prisonCode: String) = RolloutPrisonPlan(
    prisonCode = prisonCode,
    activitiesRolledOut = true,
    activitiesRolloutDate = LocalDate.now().minusDays(1),
    appointmentsRolledOut = false,
    appointmentsRolloutDate = null,
    maxDaysToExpiry = 21,
  )

  @Nested
  @DisplayName("Migrate activity")
  inner class MigrateActivity {

    private val activityCaptor = argumentCaptor<List<Activity>>()

    @BeforeEach
    fun setupMocks() {
      whenever(activityCategoryRepository.findAll()).thenReturn(listOfCategories)
      whenever(eventTierRepository.findAll()).thenReturn(listOfTiers)
      whenever(eventOrganiserRepository.findByCode("OTHER")).thenReturn(eventOrganiserOther)
      whenever(rolloutPrisonService.getByPrisonCode("MDI")).thenReturn(rolledOutMoorland)
      whenever(rolloutPrisonService.getByPrisonCode("RSI")).thenReturn(rolledOutRisley)
      whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(payBandsMoorland)
      whenever(prisonPayBandRepository.findByPrisonCode("RSI")).thenReturn(payBandsRisley)
      whenever(incentivesApiClient.getIncentiveLevelsCached(any())).thenReturn(
        listOf(
          prisonIncentiveLevel(levelCode = "BAS", levelName = "Basic"),
          prisonIncentiveLevel(levelCode = "STD", levelName = "Standard"),
          prisonIncentiveLevel(levelCode = "ENH", levelName = "Enhanced"),
          prisonIncentiveLevel(levelCode = "EN2", levelName = "Enhanced 2", active = false),
        ),
      )
      whenever(prisonApiClient.getLocation(1)).thenReturn(
        Mono.just(
          Location(
            locationId = 1,
            internalLocationCode = "011",
            description = "MDI-1-1-011",
            locationType = "N/A",
            agencyId = "RSI",
          ),
        ),
      )

      listOf("MDI", "RSI").forEach {
        whenever(prisonRegimeService.getSlotTimesForTimeSlot(it, DayOfWeek.entries.toSet(), TimeSlot.AM)).thenReturn(
          Pair(LocalTime.of(8, 30), LocalTime.of(9, 30)),
        )
        whenever(prisonRegimeService.getSlotTimesForTimeSlot(it, DayOfWeek.entries.toSet(), TimeSlot.PM)).thenReturn(
          Pair(LocalTime.of(12, 30), LocalTime.of(13, 30)),
        )
        whenever(prisonRegimeService.getSlotTimesForTimeSlot(it, DayOfWeek.entries.toSet(), TimeSlot.ED)).thenReturn(
          Pair(LocalTime.of(19, 30), LocalTime.of(20, 30)),
        )
      }
    }

    @Test
    fun `prison work - single pay rate and single time slot`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(8, 30),
          endTime = LocalTime.of(9, 30),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(eventTierRepository).findAll()
      verify(activityCategoryRepository).findAll()
      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      // Check the content of the Activity entity that was passed into saveAndFlush
      with(activityCaptor.firstValue[0]) {
        assertThat(summary).isEqualTo("An activity")
        assertThat(description).isEqualTo("An activity")
        assertThat(inCell).isFalse
        assertThat(onWing).isFalse
        assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(endDate).isNull()
        assertThat(eligibilityRules()).hasSize(0)
        assertThat(activityMinimumEducationLevel()).hasSize(0)
        assertThat(activityCategory).isEqualTo(getCategory("SAA_PRISON_JOBS"))
        assertThat(activityTier).isEqualTo(getTier("TIER_1"))
        assertThat(isPaid()).isTrue

        // Check the pay rates for this activity
        assertThat(activityPay()).hasSize(1)
        with(activityPay().first()) {
          assertThat(incentiveNomisCode).isEqualTo("BAS")
          assertThat(incentiveLevel).isEqualTo("Basic")
          assertThat(payBand.nomisPayBand).isEqualTo(1)
          assertThat(payBand.payBandAlias).isEqualTo("1")
          assertThat(payBand.payBandDescription).isEqualTo("Pay band 1")
          assertThat(rate).isEqualTo(110)
        }

        // Check the schedule attributes
        assertThat(schedules()).hasSize(1)
        with(schedules().first()) {
          assertThat(description).isEqualTo("An activity")
          assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(endDate).isNull()
          assertThat(capacity).isEqualTo(10)
          assertThat(runsOnBankHoliday).isFalse
          assertThat(internalLocationId).isEqualTo(1)
          assertThat(internalLocationCode).isEqualTo("011")
          assertThat(internalLocationDescription).isEqualTo("MDI-1-1-011")
          assertThat(scheduleWeeks).isEqualTo(1)

          // Check the slots created
          assertThat(slots()).hasSize(1)
          with(slots().first()) {
            assertThat(startTime).isEqualTo(LocalTime.of(8, 30))
            assertThat(endTime).isEqualTo(LocalTime.of(9, 30))
            assertThat(mondayFlag).isTrue
            assertThat(tuesdayFlag).isFalse
            assertThat(wednesdayFlag).isFalse
            assertThat(thursdayFlag).isFalse
            assertThat(fridayFlag).isFalse
            assertThat(saturdayFlag).isFalse
            assertThat(sundayFlag).isFalse
          }
        }
      }

      verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, 1)
    }

    @Test
    fun `prison job - multiple pay rates and time slots`() {
      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110),
        NomisPayRate(incentiveLevel = "STD", nomisPayBand = "1", rate = 120),
        NomisPayRate(incentiveLevel = "ENH", nomisPayBand = "1", rate = 130),
      )

      val nomisScheduleRules = listOf(
        NomisScheduleRule(startTime = LocalTime.of(8, 30), endTime = LocalTime.of(9, 30), monday = true),
        NomisScheduleRule(startTime = LocalTime.of(12, 30), endTime = LocalTime.of(13, 30), monday = true),
        NomisScheduleRule(startTime = LocalTime.of(19, 30), endTime = LocalTime.of(20, 30), monday = true),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(isPaid()).isTrue
        assertThat(activityPay()).hasSize(3)

        with(activityPay()[0]) {
          assertThat(incentiveNomisCode).isEqualTo("BAS")
          assertThat(incentiveLevel).isEqualTo("Basic")
          assertThat(payBand.nomisPayBand).isEqualTo(1)
          assertThat(payBand.payBandAlias).isEqualTo("1")
          assertThat(payBand.payBandDescription).isEqualTo("Pay band 1")
          assertThat(rate).isEqualTo(110)
        }

        with(activityPay()[1]) {
          assertThat(incentiveNomisCode).isEqualTo("STD")
          assertThat(incentiveLevel).isEqualTo("Standard")
          assertThat(payBand.nomisPayBand).isEqualTo(1)
          assertThat(payBand.payBandAlias).isEqualTo("1")
          assertThat(payBand.payBandDescription).isEqualTo("Pay band 1")
          assertThat(rate).isEqualTo(120)
        }

        with(activityPay()[2]) {
          assertThat(incentiveNomisCode).isEqualTo("ENH")
          assertThat(incentiveLevel).isEqualTo("Enhanced")
          assertThat(payBand.nomisPayBand).isEqualTo(1)
          assertThat(payBand.payBandAlias).isEqualTo("1")
          assertThat(payBand.payBandDescription).isEqualTo("Pay band 1")
          assertThat(rate).isEqualTo(130)
        }

        assertThat(schedules()).hasSize(1)

        with(schedules().first()) {
          assertThat(slots()).hasSize(3)

          with(slots()[0]) {
            assertThat(startTime).isEqualTo(LocalTime.of(8, 30))
            assertThat(endTime).isEqualTo(LocalTime.of(9, 30))
            assertThat(mondayFlag).isTrue
          }

          with(slots()[1]) {
            assertThat(startTime).isEqualTo(LocalTime.of(12, 30))
            assertThat(endTime).isEqualTo(LocalTime.of(13, 30))
            assertThat(mondayFlag).isTrue
          }

          with(slots()[2]) {
            assertThat(startTime).isEqualTo(LocalTime.of(19, 30))
            assertThat(endTime).isEqualTo(LocalTime.of(20, 30))
            assertThat(mondayFlag).isTrue
          }
        }
      }
    }

    @Test
    fun `education course - runs every morning mon-friday time slots`() {
      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110),
        NomisPayRate(incentiveLevel = "STD", nomisPayBand = "1", rate = 120),
        NomisPayRate(incentiveLevel = "ENH", nomisPayBand = "1", rate = 130),
      )

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(8, 30),
          endTime = LocalTime.of(9, 30),
          monday = true,
          tuesday = true,
          wednesday = true,
          thursday = true,
          friday = true,
        ),
      )

      // Switch the category to education
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        programServiceCode = "EDU_MATHS",
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(activityCategory.code).isEqualTo("SAA_EDUCATION")
        assertThat(isPaid()).isTrue
        assertThat(activityPay()).hasSize(3)
        assertThat(schedules()).hasSize(1)

        with(schedules().first()) {
          assertThat(slots()).hasSize(1)
          with(slots().first()) {
            assertThat(startTime).isEqualTo(LocalTime.of(8, 30))
            assertThat(endTime).isEqualTo(LocalTime.of(9, 30))
            assertThat(mondayFlag).isTrue
            assertThat(tuesdayFlag).isTrue
            assertThat(wednesdayFlag).isTrue
            assertThat(thursdayFlag).isTrue
            assertThat(fridayFlag).isTrue
            assertThat(saturdayFlag).isFalse
            assertThat(sundayFlag).isFalse
          }
        }
      }
    }

    @Test
    fun `An in-cell activity - no location code specified`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      // Remove the location values to indicate it is an in-cell or outside prison activity
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        internalLocationId = null,
        internalLocationCode = null,
        internalLocationDescription = null,
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(inCell).isTrue
        assertThat(isPaid()).isTrue
        assertThat(activityPay()).hasSize(1)
        assertThat(schedules()).hasSize(1)
        with(schedules().first()) {
          assertThat(slots()).hasSize(1)
        }
      }
    }

    @Test
    fun `An in-cell activity - specified by program service code T2ICA`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        programServiceCode = "T2ICA",
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(inCell).isTrue
      }
    }

    @Test
    fun `An on-wing activity - has WOW location code`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        internalLocationCode = "WOW",
        internalLocationDescription = "MDI-1-1-WOW",
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(inCell).isFalse
        assertThat(onWing).isTrue
      }
    }

    @Test
    fun `Migrating duplicate slot times consolidates them into 1 slot`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(8, 30),
          endTime = LocalTime.of(9, 30),
          monday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(8, 30),
          endTime = LocalTime.of(9, 30),
          tuesday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        runsOnBankHoliday = true,
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      service.migrateActivity(request)

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        val slot = schedules().first().slots().single()
        slot.slotTimes() isEqualTo (LocalTime.of(8, 30) to LocalTime.of(9, 30))
        slot.getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
      }
    }

    @Test
    fun `An activity which runs on a bank holiday`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        runsOnBankHoliday = true,
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        with(schedules().first()) {
          assertThat(slots()).hasSize(1)
          assertThat(runsOnBankHoliday).isTrue
        }
      }
    }

    @Test
    fun `An activity with the outside work flag set will be flagged as such`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        outsideWork = true,
      )

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        with(schedules().first()) {
          assertThat(slots()).hasSize(1)
          assertThat(outsideWork).isTrue
        }
      }
    }

    @Test
    fun `No pay rates provided will result in an unpaid activity being created`() {
      val nomisPayRates: List<NomisPayRate> = emptyList()
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(isPaid()).isFalse
        assertThat(activityPay()).isNullOrEmpty()
      }
    }

    @Test
    fun `An unrecognised NOMIS pay band will fail the migration of an activity`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "11", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      val exception = assertThrows<ValidationException> {
        service.migrateActivity(request)
      }

      assertThat(exception.message).contains("Failed to migrate activity")
      assertThat(exception.message).contains("No prison pay band for Nomis pay band 11")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(prisonPayBandRepository).findByPrisonCode("MDI")

      verify(activityScheduleRepository, times(0)).saveAllAndFlush(anyList())
    }

    @Test
    fun `Fails if no incentive levels are found for prison`() {
      whenever(incentivesApiClient.getIncentiveLevelsCached(any())).thenReturn(emptyList())

      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 100),
      )

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      val exception = assertThrows<ValidationException> {
        service.migrateActivity(request)
      }

      assertThat(exception.message).contains("No incentive levels found for the requested prison ${request.prisonCode}")
      verify(activityRepository, times(0)).saveAllAndFlush(anyList())
    }

    @Test
    fun `Any unrecognised incentive level codes in pay rates will fail`() {
      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 100),
        NomisPayRate(incentiveLevel = "STD", nomisPayBand = "1", rate = 100),
        NomisPayRate(incentiveLevel = "ENH", nomisPayBand = "1", rate = 100),
        // Unrecognised IEP
        NomisPayRate(incentiveLevel = "XXX", nomisPayBand = "1", rate = 110),
      )

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      val exception = assertThrows<ValidationException> {
        service.migrateActivity(request)
      }

      assertThat(exception.message).contains("Failed to migrate activity ${request.description}. Activity incentive level XXX is not active in this prison")
      verify(activityRepository, times(0)).saveAllAndFlush(anyList())
    }

    @Test
    fun `Any inactive incentive level codes in pay rates will fail`() {
      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 100),
        NomisPayRate(incentiveLevel = "STD", nomisPayBand = "1", rate = 100),
        NomisPayRate(incentiveLevel = "ENH", nomisPayBand = "1", rate = 100),
        // Inactive IEP
        NomisPayRate(incentiveLevel = "EN2", nomisPayBand = "1", rate = 120),
      )

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      val exception = assertThrows<ValidationException> {
        service.migrateActivity(request)
      }

      assertThat(exception.message).contains("Failed to migrate activity ${request.description}. Activity incentive level EN2 is not active in this prison")
      verify(activityRepository, times(0)).saveAllAndFlush(anyList())
    }

    @Test
    fun `A tier2 activity will have a default organiser`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 100))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      // Make the program service match a TIER_2 type
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(programServiceCode = "T2ICA")

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(activityTier.code).isEqualTo("TIER_2")
        assertThat(organiser?.code).isEqualTo("OTHER")
      }
    }

    @Test
    fun `A foundational tier activity will NOT have a default organiser`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 100))

      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          monday = true,
        ),
      )

      // Make the program service match a FOUNDATION tier
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(programServiceCode = "UNEMP")

      whenever(activityRepository.saveAllAndFlush(anyList())).thenReturn(listOf(activityEntity()))

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAllAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue[0]) {
        assertThat(activityTier.code).isEqualTo("FOUNDATION")
        assertThat(organiser).isNull()
      }
    }

    private fun buildActivityMigrateRequest(
      payRates: List<NomisPayRate> = emptyList(),
      scheduleRules: List<NomisScheduleRule> = emptyList(),
    ) =
      ActivityMigrateRequest(
        programServiceCode = "CLNR",
        prisonCode = "MDI",
        startDate = LocalDate.now().minusDays(1),
        endDate = null,
        internalLocationId = 1,
        internalLocationCode = "011",
        internalLocationDescription = "MDI-1-1-011",
        capacity = 10,
        description = "An activity",
        payPerSession = "H",
        runsOnBankHoliday = false,
        outsideWork = false,
        scheduleRules,
        payRates,
      )
  }

  @Nested
  @DisplayName("Migrate allocation")
  inner class MigrateAllocation {
    private val activityScheduleCaptor = argumentCaptor<ActivitySchedule>()

    private val prisonerDetail = Prisoner(
      prisonerNumber = "A124BB",
      firstName = "Bob",
      lastName = "Bobson",
      dateOfBirth = LocalDate.of(1999, 10, 1),
      gender = "M",
      status = "ACTIVE IN",
      bookingId = "1",
      prisonId = "MDI",
    )

    private val inactivePrisonerDetail = Prisoner(
      prisonerNumber = "A124BB",
      firstName = "Bob",
      lastName = "Bobson",
      dateOfBirth = LocalDate.of(1999, 10, 1),
      gender = "M",
      status = "INACTIVE OUT",
      bookingId = "1",
      prisonId = "MDI",
    )

    @BeforeEach
    fun setupMocks() {
      reset(rolloutPrisonService, prisonPayBandRepository, activityRepository, activityScheduleRepository, prisonerSearchApiClient)
      whenever(rolloutPrisonService.getByPrisonCode("MDI")).thenReturn(rolledOutMoorland)
      whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(payBandsMoorland)
      whenever(activityRepository.findByActivityIdAndPrisonCode(1L, "MDI")).thenReturn(activityEntity())
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(activityEntity().schedules().first())
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(any(), any())).thenReturn(listOf(prisonerDetail))
    }

    @Test
    fun `A prisoner will be allocated from tomorrow in a PENDING status`() {
      val request = buildAllocationMigrateRequest()
      val schedule = activityEntity().schedules().first()

      // The expected allocation - required so final check succeeds
      schedule.allocatePrisoner(
        prisonerNumber = "A1234BB".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 1,
        allocatedBy = MIGRATION_USER,
        startDate = LocalDate.now().plusDays(1),
        endDate = null,
      )

      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      val response = service.migrateAllocation(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.allocationId).isEqualTo(0L) // default

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerNumber).isEqualTo("A1234BB")
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(payBand?.nomisPayBand).isEqualTo("1".toInt())
          assertThat(endDate).isNull()
        }
      }

      verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATED, 0)
    }

    @Test
    fun `A suspended prisoner in NOMIS will be allocated from tomorrow as PENDING with a planned suspension starting immediately`() {
      val request = buildAllocationMigrateRequest().copy(
        suspendedFlag = true,
      )

      val schedule = activityEntity().schedules().first()

      // The expected allocation - required so final check succeeds
      schedule.allocatePrisoner(
        prisonerNumber = "A1234BB".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 1,
        allocatedBy = MIGRATION_USER,
        startDate = LocalDate.now().plusDays(1),
        endDate = null,
      )

      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      val response = service.migrateAllocation(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.allocationId).isEqualTo(0) // default

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(plannedSuspension()!!.startDate()).isEqualTo(LocalDate.now().plusDays(1))
        }
      }
    }

    @Test
    fun `A prisoner with a future start date will be allocated from their start date as PENDING`() {
      val request = buildAllocationMigrateRequest().copy(
        startDate = LocalDate.now().plusDays(10),
      )

      val schedule = activityEntity().schedules().first()

      // The expected allocation - required so final check succeeds
      schedule.allocatePrisoner(
        prisonerNumber = "A1234BB".toPrisonerNumber(),
        startDate = LocalDate.now().plusDays(1),
        payBand = lowPayBand,
        bookingId = 1,
        allocatedBy = MIGRATION_USER,
      )

      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      val response = service.migrateAllocation(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.allocationId).isEqualTo(0) // default

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(startDate).isEqualTo(LocalDate.now().plusDays(10))
        }
      }
    }

    @Test
    fun `A prisoner with an end date is allocated with the end date and a planned deallocation`() {
      val request = buildAllocationMigrateRequest().copy(
        startDate = LocalDate.now().plusDays(10),
        endDate = LocalDate.now().plusDays(20),
      )

      val schedule = activityEntity().schedules().first()

      // The expected allocation - required so final check succeeds
      schedule.allocatePrisoner(
        prisonerNumber = "A1234BB".toPrisonerNumber(),
        startDate = LocalDate.now().plusDays(1),
        payBand = lowPayBand,
        bookingId = 1,
        allocatedBy = MIGRATION_USER,
      )

      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      val response = service.migrateAllocation(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.allocationId).isEqualTo(0) // default

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(startDate).isEqualTo(LocalDate.now().plusDays(10))
          assertThat(endDate).isEqualTo(LocalDate.now().plusDays(20))
          assertThat(plannedDeallocation?.plannedDate).isEqualTo(LocalDate.now().plusDays(20))
          assertThat(plannedDeallocation?.plannedBy).isEqualTo(MIGRATION_USER)
          assertThat(plannedDeallocation?.plannedReason).isNotNull
        }
      }
    }

    @Test
    fun `A prisoner who is inactive at the prison will fail to be allocated`() {
      val request = buildAllocationMigrateRequest()

      whenever(prisonerSearchApiClient.findByPrisonerNumbers(any(), any())).thenReturn(listOf(inactivePrisonerDetail))

      val exception = assertThrows<ValidationException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).isEqualTo("Allocation failed A1234BB. Prisoner not in MDI or INACTIVE")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityRepository).findByActivityIdAndPrisonCode(1, "MDI")
      verify(activityScheduleRepository).findBy(1, "MDI")
      verify(prisonerSearchApiClient).findByPrisonerNumbers(listOf("A1234BB"))

      verify(prisonPayBandRepository, times(0)).findByPrisonCode("MDI")
      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `Activity is not active tomorrow will fail`() {
      val request = buildAllocationMigrateRequest()

      // Set the activity to start in 2 days time
      whenever(activityRepository.findByActivityIdAndPrisonCode(1L, "MDI"))
        .thenReturn(
          activityEntity().apply {
            startDate = LocalDate.now().plusDays(2)
          },
        )

      val exception = assertThrows<ValidationException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).contains("Allocation failed A1234BB")
      assertThat(exception.message).contains("activity is not active tomorrow")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityRepository).findByActivityIdAndPrisonCode(1, "MDI")

      verify(activityScheduleRepository, times(0)).findBy(1, "MDI")
      verify(prisonerSearchApiClient, times(0)).findByPrisonerNumbers(listOf("A1234BB"))
      verify(prisonPayBandRepository, times(0)).findByPrisonCode("MDI")
      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `Activity schedule is not active tomorrow will fail`() {
      val request = buildAllocationMigrateRequest()

      // Set the schedule to start in 2 days time
      whenever(activityScheduleRepository.findBy(any(), any()))
        .thenReturn(
          activityEntity().schedules().first().apply {
            startDate = LocalDate.now().plusDays(2)
          },
        )

      val exception = assertThrows<ValidationException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).contains("Allocation failed A1234BB")
      assertThat(exception.message).contains("schedule is not active tomorrow")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityRepository).findByActivityIdAndPrisonCode(1, "MDI")
      verify(activityScheduleRepository).findBy(1, "MDI")

      verify(prisonerSearchApiClient, times(0)).findByPrisonerNumbers(listOf("A1234BB"))
      verify(prisonPayBandRepository, times(0)).findByPrisonCode("MDI")
      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `Already allocated prisoner will fail`() {
      // This prisoner is already allocated to the activity
      val request = buildAllocationMigrateRequest().copy(
        prisonerNumber = "A1234AA",
      )

      val exception = assertThrows<ValidationException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).contains("Allocation failed A1234AA")
      assertThat(exception.message).contains("Already allocated")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityRepository).findByActivityIdAndPrisonCode(1, "MDI")
      verify(activityScheduleRepository).findBy(1, "MDI")

      verify(prisonerSearchApiClient, times(0)).findByPrisonerNumbers(listOf("A1234AA"))
      verify(prisonPayBandRepository, times(0)).findByPrisonCode("MDI")
      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `Allocation with no pay band will succeed for an unpaid activity`() {
      val request = buildAllocationMigrateRequest().copy(
        nomisPayBand = null,
      )

      val activity = activityEntity(paid = false, noPayBands = true)
      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      service.migrateAllocation(request)

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(payBand).isNull()
        }
      }
    }

    @Test
    fun `Allocation with a valid pay band will succeed for an unpaid activity - with a null pay band`() {
      val request = buildAllocationMigrateRequest().copy(
        nomisPayBand = "1",
      )

      val activity = activityEntity(paid = false, noPayBands = true)
      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      service.migrateAllocation(request)

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(payBand).isNull()
        }
      }
    }

    @Test
    fun `Allocation with pay band which is valid for the prison but not on the activity pay rates will fail`() {
      val request = buildAllocationMigrateRequest().copy(
        nomisPayBand = "3",
      )

      val activity = activityEntity()
      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      val exception = assertThrows<ValidationException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).contains("Allocation failed A1234BB")
      assertThat(exception.message).contains("Nomis pay band 3 is not on a pay rate")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityRepository).findByActivityIdAndPrisonCode(1, "MDI")
      verify(activityScheduleRepository).findBy(1, "MDI")
      verify(prisonerSearchApiClient).findByPrisonerNumbers(listOf("A1234BB"))
      verify(prisonPayBandRepository).findByPrisonCode("MDI")

      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `No pay band provided for a paid activity - will be allocated with the lowest rate pay band`() {
      val request = buildAllocationMigrateRequest().copy(
        nomisPayBand = null,
      )

      val activity = activityEntity()
      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      service.migrateAllocation(request)

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(payBand?.prisonPayBandId).isEqualTo(lowPayBand.prisonPayBandId)
        }
      }
    }

    @Test
    fun `An invalid pay band for this prison will always fail`() {
      // Pay band 12 is not configured for the prison
      val request = buildAllocationMigrateRequest().copy(
        nomisPayBand = "12",
      )

      val exception = assertThrows<ValidationException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).isEqualTo("Allocation failed A1234BB. Nomis pay band 12 is not configured for MDI")

      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityRepository).findByActivityIdAndPrisonCode(1, "MDI")
      verify(activityScheduleRepository).findBy(1, "MDI")
      verify(prisonerSearchApiClient).findByPrisonerNumbers(listOf("A1234BB"))
      verify(prisonPayBandRepository).findByPrisonCode("MDI")

      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    @Test
    fun `A valid exclusion provided will be applied to the prisoner as part of the allocation`() {
      val request = buildAllocationMigrateRequest().copy(
        exclusions = listOf(Slot(weekNumber = 1, timeSlot = "AM", monday = true)),
      )

      val activity = activityEntity(noSchedules = true).apply {
        addSchedule(activitySchedule(this, noSlots = true)).apply {
          addSlot(
            weekNumber = 1,
            slotTimes = LocalTime.of(10, 0) to LocalTime.of(11, 0),
            setOf(DayOfWeek.MONDAY),
          )
        }
      }

      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      service.migrateAllocation(request)

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          assertThat(prisonerStatus).isEqualTo(PrisonerStatus.PENDING)
          assertThat(exclusions(ExclusionsFilter.ACTIVE)).hasSize(1)
          with(exclusions(ExclusionsFilter.ACTIVE).first()) {
            getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
            assertThat(timeSlot).isEqualTo(TimeSlot.AM)
          }
        }
      }
    }

    @Test
    fun `Multiple exclusions for common timeSlot and weekNumber pairs will be applied to the correct slots`() {
      val request = buildAllocationMigrateRequest().copy(
        exclusions = listOf(
          Slot(weekNumber = 1, timeSlot = "AM", monday = true),
          Slot(weekNumber = 1, timeSlot = "AM", tuesday = true),
          Slot(weekNumber = 1, timeSlot = "PM", wednesday = true),
          Slot(weekNumber = 2, timeSlot = "PM", thursday = true),
        ),
      )

      val activity = activityEntity(noSchedules = true).apply {
        addSchedule(activitySchedule(this, noSlots = true, scheduleWeeks = 2)).apply {
          addSlot(
            weekNumber = 1,
            slotTimes = LocalTime.of(10, 0) to LocalTime.of(11, 0),
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
          )
          addSlot(
            weekNumber = 1,
            slotTimes = LocalTime.of(11, 0) to LocalTime.of(12, 0),
            setOf(DayOfWeek.MONDAY),
          )
          addSlot(
            weekNumber = 1,
            slotTimes = LocalTime.of(13, 0) to LocalTime.of(14, 0),
            setOf(DayOfWeek.WEDNESDAY),
          )
          addSlot(
            weekNumber = 2,
            slotTimes = LocalTime.of(13, 0) to LocalTime.of(14, 0),
            setOf(DayOfWeek.THURSDAY),
          )
        }
      }

      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      service.migrateAllocation(request)

      verify(activityScheduleRepository).saveAndFlush(activityScheduleCaptor.capture())

      with(activityScheduleCaptor.firstValue) {
        with(allocations().last()) {
          with(exclusions(ExclusionsFilter.ACTIVE)) {
            this hasSize 4

            this.elementAt(0).weekNumber isEqualTo 1
            this.elementAt(0).slotTimes() isEqualTo (LocalTime.of(10, 0) to LocalTime.of(11, 0))
            this.elementAt(0).getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)

            this.elementAt(1).weekNumber isEqualTo 1
            this.elementAt(1).slotTimes() isEqualTo (LocalTime.of(11, 0) to LocalTime.of(12, 0))
            this.elementAt(1).getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)

            this.elementAt(2).weekNumber isEqualTo 1
            this.elementAt(2).slotTimes() isEqualTo (LocalTime.of(13, 0) to LocalTime.of(14, 0))
            this.elementAt(2).getDaysOfWeek() isEqualTo setOf(DayOfWeek.WEDNESDAY)

            this.elementAt(3).weekNumber isEqualTo 2
            this.elementAt(3).slotTimes() isEqualTo (LocalTime.of(13, 0) to LocalTime.of(14, 0))
            this.elementAt(3).getDaysOfWeek() isEqualTo setOf(DayOfWeek.THURSDAY)
          }
        }
      }
    }

    @Test
    fun `Exclusions which do not match slots in the schedule will fail`() {
      val request = buildAllocationMigrateRequest().copy(
        exclusions = listOf(Slot(weekNumber = 1, timeSlot = "PM", friday = true)),
      )

      val activity = activityEntity(noSchedules = true).apply {
        addSchedule(activitySchedule(this, noSlots = true)).apply {
          addSlot(
            weekNumber = 1,
            slotTimes = LocalTime.of(10, 0) to LocalTime.of(11, 0),
            setOf(DayOfWeek.MONDAY),
          )
        }
      }

      val schedule = activity.schedules().first()

      whenever(activityRepository.findByActivityIdAndPrisonCode(1, "MDI")).thenReturn(activity)
      whenever(activityScheduleRepository.findBy(any(), any())).thenReturn(schedule)
      whenever(activityScheduleRepository.saveAndFlush(any())).thenReturn(schedule)

      val exception = assertThrows<IllegalArgumentException> {
        service.migrateAllocation(request)
      }

      assertThat(exception.message).contains("No PM slots in week number 1")

      verify(activityScheduleRepository, times(0)).saveAndFlush(any())
    }

    private fun buildAllocationMigrateRequest() =
      AllocationMigrateRequest(
        prisonCode = "MDI",
        activityId = 1,
        splitRegimeActivityId = null,
        prisonerNumber = "A1234BB",
        bookingId = 1,
        cellLocation = "MDI-1-1-001",
        nomisPayBand = "1",
        startDate = LocalDate.now().minusDays(1),
        endDate = null,
        endComment = null,
        suspendedFlag = false,
      )
  }

  @Nested
  @DisplayName("Nomis program service code mapping to activity categories")
  inner class CategoryMapping {

    @BeforeEach
    fun setupMocks() {
      whenever(activityCategoryRepository.findAll()).thenReturn(listOfCategories)
    }

    @Test
    fun `Prison industries`() {
      assertThat(service.mapProgramToCategory("IND_DTP")).isEqualTo(getCategory("SAA_INDUSTRIES"))
      assertThat(service.mapProgramToCategory("IND_INT")).isEqualTo(getCategory("SAA_INDUSTRIES"))
      assertThat(service.mapProgramToCategory("IND_TEXT")).isEqualTo(getCategory("SAA_INDUSTRIES"))
      assertThat(service.mapProgramToCategory("IND_ASA")).isEqualTo(getCategory("SAA_INDUSTRIES"))
      assertThat(service.mapProgramToCategory("IND_CRTY")).isEqualTo(getCategory("SAA_INDUSTRIES"))
      assertThat(service.mapProgramToCategory("IND_LAU")).isEqualTo(getCategory("SAA_INDUSTRIES"))
    }

    @Test
    fun `Services and prison jobs`() {
      assertThat(service.mapProgramToCategory("SER_")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
      assertThat(service.mapProgramToCategory("CLNR")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
      assertThat(service.mapProgramToCategory("CLNR_TYPE1")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
      assertThat(service.mapProgramToCategory("CLNR_TYPE2")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
      assertThat(service.mapProgramToCategory("KITCHEN")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
      assertThat(service.mapProgramToCategory("LIBRARY")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
      assertThat(service.mapProgramToCategory("FG")).isEqualTo(getCategory("SAA_PRISON_JOBS"))
    }

    @Test
    fun `Education activities`() {
      assertThat(service.mapProgramToCategory("EDUACC")).isEqualTo(getCategory("SAA_EDUCATION"))
      assertThat(service.mapProgramToCategory("CORECLASS")).isEqualTo(getCategory("SAA_EDUCATION"))
      assertThat(service.mapProgramToCategory("SKILLS")).isEqualTo(getCategory("SAA_EDUCATION"))
      assertThat(service.mapProgramToCategory("KEY_SKILLS")).isEqualTo(getCategory("SAA_EDUCATION"))
    }

    @Test
    fun `Not in work`() {
      assertThat(service.mapProgramToCategory("UNEMP")).isEqualTo(getCategory("SAA_NOT_IN_WORK"))
      assertThat(service.mapProgramToCategory("OTH_UNE")).isEqualTo(getCategory("SAA_NOT_IN_WORK"))
    }

    @Test
    fun `Intervention activities`() {
      assertThat(service.mapProgramToCategory("INT_")).isEqualTo(getCategory("SAA_INTERVENTIONS"))
      assertThat(service.mapProgramToCategory("GROUP")).isEqualTo(getCategory("SAA_INTERVENTIONS"))
      assertThat(service.mapProgramToCategory("ABUSE")).isEqualTo(getCategory("SAA_INTERVENTIONS"))
    }

    @Test
    fun `Gym, sports and fitness activities`() {
      assertThat(service.mapProgramToCategory("PE_TYPE1")).isEqualTo(getCategory("SAA_GYM_SPORTS_FITNESS"))
      assertThat(service.mapProgramToCategory("PE_TYPE2")).isEqualTo(getCategory("SAA_GYM_SPORTS_FITNESS"))
      assertThat(service.mapProgramToCategory("SPORT")).isEqualTo(getCategory("SAA_GYM_SPORTS_FITNESS"))
      assertThat(service.mapProgramToCategory("HEALTH")).isEqualTo(getCategory("SAA_GYM_SPORTS_FITNESS"))
      assertThat(service.mapProgramToCategory("OTH_PER")).isEqualTo(getCategory("SAA_GYM_SPORTS_FITNESS"))
    }

    @Test
    fun `Induction activities`() {
      assertThat(service.mapProgramToCategory("INDUCTION")).isEqualTo(getCategory("SAA_INDUCTION"))
      assertThat(service.mapProgramToCategory("IAG")).isEqualTo(getCategory("SAA_INDUCTION"))
      assertThat(service.mapProgramToCategory("SAFE")).isEqualTo(getCategory("SAA_INDUCTION"))
    }

    @Test
    fun `Faith and religion activities`() {
      assertThat(service.mapProgramToCategory("CHAP")).isEqualTo(getCategory("SAA_FAITH_SPIRITUALITY"))
      assertThat(service.mapProgramToCategory("T2CFA")).isEqualTo(getCategory("SAA_FAITH_SPIRITUALITY"))
      assertThat(service.mapProgramToCategory("OTH_CFR")).isEqualTo(getCategory("SAA_FAITH_SPIRITUALITY"))
    }

    @Test
    fun `Other type activities`() {
      assertThat(service.mapProgramToCategory("RANDOM TYPE")).isEqualTo(getCategory("SAA_OTHER"))
      assertThat(service.mapProgramToCategory("T2ICA")).isEqualTo(getCategory("SAA_OTHER"))
      assertThat(service.mapProgramToCategory("OTRESS")).isEqualTo(getCategory("SAA_OTHER"))
    }
  }

  @Nested
  @DisplayName("Nomis program service to activity tier mapping")
  inner class TierMapping {

    @BeforeEach
    fun setupMocks() {
      whenever(eventTierRepository.findAll()).thenReturn(listOfTiers)
    }

    @Test
    fun `Prison services are tier 1`() {
      assertThat(service.mapProgramToTier("SER_ORD")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Cleaning is tier 1`() {
      assertThat(service.mapProgramToTier("CLNR_HU4")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Training is tier 1`() {
      assertThat(service.mapProgramToTier("SKILLS")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Education is tier 1`() {
      assertThat(service.mapProgramToTier("EDU_OLA")).isEqualTo(getTier("TIER_1"))
      assertThat(service.mapProgramToTier("EDU_NLA")).isEqualTo(getTier("TIER_1"))
      assertThat(service.mapProgramToTier("KEY_SKILLS")).isEqualTo(getTier("TIER_1"))
      assertThat(service.mapProgramToTier("CORECLASS")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Prison industries are tier 1`() {
      assertThat(service.mapProgramToTier("IND_DTP")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Other occupations are tier 1`() {
      assertThat(service.mapProgramToTier("OTHOCC")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Induction is tier 1`() {
      assertThat(service.mapProgramToTier("INDUCTION")).isEqualTo(getTier("TIER_1"))
      assertThat(service.mapProgramToTier("IAG")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Interventions are tier 1`() {
      assertThat(service.mapProgramToTier("GROUP")).isEqualTo(getTier("TIER_1"))
      assertThat(service.mapProgramToTier("ABUSE")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Safer custody is tier 1`() {
      assertThat(service.mapProgramToTier("SAFE")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Weekend activities are tier 2`() {
      assertThat(service.mapProgramToTier("WEEKEND")).isEqualTo(getTier("TIER_2"))
    }

    @Test
    fun `Other resettlement activities are tier 1`() {
      assertThat(service.mapProgramToTier("OTRESS")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Chapel and faith is tier 1`() {
      assertThat(service.mapProgramToTier("CHAPEL")).isEqualTo(getTier("TIER_1"))
    }

    @Test
    fun `Domestics are foundational`() {
      assertThat(service.mapProgramToTier("OTH_DOM")).isEqualTo(getTier("FOUNDATION"))
    }

    @Test
    fun `Association is foundational`() {
      assertThat(service.mapProgramToTier("ASSOC")).isEqualTo(getTier("FOUNDATION"))
    }

    @Test
    fun `Unemployed is foundational`() {
      assertThat(service.mapProgramToTier("OTH_UNE")).isEqualTo(getTier("FOUNDATION"))
    }

    @Test
    fun `Segregation is foundation`() {
      assertThat(service.mapProgramToTier("OTH_SEG")).isEqualTo(getTier("FOUNDATION"))
    }

    @Test
    fun `Specific T2 services map to tier 2`() {
      assertThat(service.mapProgramToTier("T2ICA")).isEqualTo(getTier("TIER_2"))
      assertThat(service.mapProgramToTier("T2PER")).isEqualTo(getTier("TIER_2"))
      assertThat(service.mapProgramToTier("T2HCW")).isEqualTo(getTier("TIER_2"))
      assertThat(service.mapProgramToTier("T2CFA")).isEqualTo(getTier("TIER_2"))
      assertThat(service.mapProgramToTier("T2OTH")).isEqualTo(getTier("TIER_2"))
    }

    @Test
    fun `Unrecognised program services default to tier 2`() {
      assertThat(service.mapProgramToTier("ANY")).isEqualTo(getTier("TIER_2"))
    }
  }

  @Nested
  @DisplayName("Nomis schedule to days of week mapping")
  inner class DaysOfTheWeekMapping {

    @Test
    fun `Nomis schedule - Monday only`() {
      val nomisScheduleRule = NomisScheduleRule(
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusMinutes(60),
        monday = true,
      )
      assertThat(service.getRequestDaysOfWeek(nomisScheduleRule)).isEqualTo(setOf(DayOfWeek.MONDAY))
    }

    @Test
    fun `Nomis schedule - Saturday and Sunday only`() {
      val nomisScheduleRule = NomisScheduleRule(
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusMinutes(60),
        saturday = true,
        sunday = true,
      )
      assertThat(service.getRequestDaysOfWeek(nomisScheduleRule)).isEqualTo(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
    }

    @Test
    fun `Nomis schedule - Wednesday only`() {
      val nomisScheduleRule = NomisScheduleRule(
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusMinutes(60),
        wednesday = true,
      )
      assertThat(service.getRequestDaysOfWeek(nomisScheduleRule)).isEqualTo(setOf(DayOfWeek.WEDNESDAY))
    }

    @Test
    fun `Nomis schedule - every day in the week`() {
      val nomisScheduleRule = NomisScheduleRule(
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusMinutes(60),
        monday = true,
        tuesday = true,
        wednesday = true,
        thursday = true,
        friday = true,
        saturday = true,
        sunday = true,
      )
      assertThat(service.getRequestDaysOfWeek(nomisScheduleRule))
        .isEqualTo(
          setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
          ),
        )
    }

    @Test
    fun `Nomis schedule - no days specified`() {
      val nomisScheduleRule = NomisScheduleRule(startTime = LocalTime.now(), endTime = LocalTime.now().plusMinutes(60))
      assertThat(service.getRequestDaysOfWeek(nomisScheduleRule)).isEmpty()
    }
  }

  @Nested
  @DisplayName("Split regime name test")
  inner class SplitRegimeName {
    @Test
    fun `Not a split regime - use the description provided`() {
      val description = "Standard name"
      assertThat(service.makeNameWithCohortLabel(false, "LEI", description, null))
        .isEqualTo(description)
    }

    @Test
    fun `Not a split regime but is Risley - use the description provided`() {
      val description = "Standard name"
      assertThat(service.makeNameWithCohortLabel(false, "RSI", description, null))
        .isEqualTo(description)
    }

    @Test
    fun `Is a split regime but not Risley - use the default cohort label`() {
      val description = "Standard name SPLIT"
      assertThat(service.makeNameWithCohortLabel(true, "LEI", description, 1))
        .isEqualTo("Standard name group 1")
    }

    @Test
    fun `Is Risley but description does not contain SPLIT - use Risley cohort label`() {
      val description = "Standard name"
      assertThat(service.makeNameWithCohortLabel(true, "RSI", description, 1))
        .isEqualTo("Standard name group 1")
    }

    @Test
    fun `Is a split regime and matches the pattern - use Risley cohort label and remove SPLIT label`() {
      val description = "Maths level one SPLIT"
      assertThat(service.makeNameWithCohortLabel(true, "RSI", description, 1))
        .isEqualTo("Maths level one group 1")
    }

    @Test
    fun `Is a split regime and matches pattern - generate a new name for cohort 2`() {
      val description = "Maths level two SPLIT"
      assertThat(service.makeNameWithCohortLabel(true, "RSI", description, 2))
        .isEqualTo("Maths level two group 2")
    }

    @Test
    fun `Is a split regime and matches split token - generate a new name for cohort 1`() {
      val description = "Split regime SPLIT"
      assertThat(service.makeNameWithCohortLabel(true, "RSI", description, 1))
        .isEqualTo("Split regime group 1")
    }

    @Test
    fun `Is Risley and split regime  - truncates a long activity name to 50 chars`() {
      val description = "0123456789 0123456789 0123456789 01234567890 0123456789 SPLIT"
      assertThat(service.makeNameWithCohortLabel(true, "RSI", description, 1))
        .isEqualTo("0123456789 0123456789 0123456789 0123456789 group 1")
    }
  }
}
