package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MigrateActivityServiceTest {
  private val rolloutPrisonService: RolloutPrisonService = mock()
  private val activityRepository: ActivityRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val activityTierRepository: ActivityTierRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()

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
    ActivityTier(1, "Tier1", "Tier one"),
  )

  private val rolledOutPrison = RolloutPrisonPlan(
    prisonCode = "MDI",
    activitiesRolledOut = true,
    activitiesRolloutDate = LocalDate.now().minusDays(1),
    appointmentsRolledOut = false,
    appointmentsRolloutDate = null,
  )

  private val payBands = listOf(
    PrisonPayBand(1L, "MDI", 1, "1", "Pay band 1", 1),
    PrisonPayBand(2L, "MDI", 2, "2", "Pay band 2", 2),
    PrisonPayBand(3L, "MDI", 3, "3", "Pay band 3", 3),
    PrisonPayBand(4L, "MDI", 4, "4", "Pay band 4", 4),
    PrisonPayBand(5L, "MDI", 5, "5", "Pay band 5", 5),
    PrisonPayBand(6L, "MDI", 6, "6", "Pay band 6", 6),
    PrisonPayBand(7L, "MDI", 7, "7", "Pay band 7", 7),
    PrisonPayBand(8L, "MDI", 8, "8", "Pay band 8", 8),
    PrisonPayBand(9L, "MDI", 9, "9", "Pay band 9", 9),
    PrisonPayBand(10L, "MDI", 10, "10", "Pay band 10", 10),
  )

  private val service = MigrateActivityService(
    rolloutPrisonService,
    activityRepository,
    activityScheduleRepository,
    activityTierRepository,
    activityCategoryRepository,
    prisonPayBandRepository,
  )

  private fun getCategory(code: String): ActivityCategory? = listOfCategories.find { it.code == code }

  private fun getTier(code: String): ActivityTier? = listOfTiers.find { it.code == code }

  @Nested
  @DisplayName("Activity mapping")
  inner class ActivityMapping {

    private val activityCaptor = argumentCaptor<Activity>()

    @BeforeEach
    fun setupMocks() {
      whenever(activityCategoryRepository.findAll()).thenReturn(listOfCategories)
      whenever(activityTierRepository.findAll()).thenReturn(listOfTiers)
      whenever(rolloutPrisonService.getByPrisonCode("MDI")).thenReturn(rolledOutPrison)
      whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(payBands)
    }

    @Test
    fun `prison job - single pay rate and single time slot`() {
      // Build the migration request object
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(NomisScheduleRule(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), monday = true))
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      // Catch the Activity entity that is saved (and return some standard activity(
      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      // Check the response - a dummy activity
      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      // Verify the calls made
      verify(rolloutPrisonService).getByPrisonCode("MDI")
      verify(activityTierRepository).findAll()
      verify(activityCategoryRepository).findAll()
      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      // Check the content of the saved Activity entity
      with(activityCaptor.firstValue) {
        assertThat(summary).isEqualTo("An activity")
        assertThat(description).contains("Migrated from NOMIS")
        assertThat(inCell).isFalse
        assertThat(onWing).isFalse
        assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(endDate).isNull()
        assertThat(eligibilityRules()).hasSize(0)
        assertThat(activityMinimumEducationLevel()).hasSize(0)
        assertThat(activityCategory).isEqualTo(getCategory("SAA_PRISON_JOBS"))
        assertThat(activityTier).isEqualTo(getTier("Tier1"))

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
            assertThat(startTime).isEqualTo(LocalTime.of(10, 0))
            assertThat(endTime).isEqualTo(LocalTime.of(11, 0))
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
    }

    @Test
    fun `prison job - multiple pay rates and time slots`() {
      // Multiple pay rates
      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110),
        NomisPayRate(incentiveLevel = "STD", nomisPayBand = "1", rate = 120),
        NomisPayRate(incentiveLevel = "ENH", nomisPayBand = "1", rate = 130),
      )

      // Multiple time slots
      val nomisScheduleRules = listOf(
        NomisScheduleRule(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), monday = true),
        NomisScheduleRule(startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 0), monday = true),
        NomisScheduleRule(startTime = LocalTime.of(18, 0), endTime = LocalTime.of(19, 0), monday = true),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)

      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      // Capture the activity saved
      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue) {
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
            assertThat(startTime).isEqualTo(LocalTime.of(10, 0))
            assertThat(endTime).isEqualTo(LocalTime.of(11, 0))
            assertThat(mondayFlag).isTrue
          }

          with(slots()[1]) {
            assertThat(startTime).isEqualTo(LocalTime.of(13, 0))
            assertThat(endTime).isEqualTo(LocalTime.of(14, 0))
            assertThat(mondayFlag).isTrue
          }

          with(slots()[2]) {
            assertThat(startTime).isEqualTo(LocalTime.of(18, 0))
            assertThat(endTime).isEqualTo(LocalTime.of(19, 0))
            assertThat(mondayFlag).isTrue
          }
        }
      }
    }

    @Test
    fun `education course - runs every morning mon-friday time slots`() {
      // Multiple pay rates
      val nomisPayRates = listOf(
        NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110),
        NomisPayRate(incentiveLevel = "STD", nomisPayBand = "1", rate = 120),
        NomisPayRate(incentiveLevel = "ENH", nomisPayBand = "1", rate = 130),
      )

      // Mornings - every weekday
      val nomisScheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
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

      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      // Capture the activity saved
      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue) {
        assertThat(activityCategory.code).isEqualTo("SAA_EDUCATION")
        assertThat(activityPay()).hasSize(3)
        assertThat(schedules()).hasSize(1)

        with(schedules().first()) {
          assertThat(slots()).hasSize(1)

          with(slots().first()) {
            assertThat(startTime).isEqualTo(LocalTime.of(10, 0))
            assertThat(endTime).isEqualTo(LocalTime.of(11, 0))
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
        NomisScheduleRule(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), monday = true),
      )

      // Remove the location values
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        internalLocationId = null,
        internalLocationCode = null,
        internalLocationDescription = null,
      )

      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      // Capture the activity saved
      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue) {
        assertThat(inCell).isTrue
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
        NomisScheduleRule(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), monday = true),
      )

      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        programServiceCode = "T2ICA",
      )

      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      // Capture the activity saved
      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue) {
        assertThat(inCell).isTrue
      }
    }

    @Test
    fun `An on-wing activity - has WOW location code`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), monday = true),
      )

      // Has a WOW location code
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        internalLocationCode = "WOW",
        internalLocationDescription = "MDI-1-1-WOW",
      )

      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue) {
        assertThat(inCell).isFalse
        assertThat(onWing).isTrue
      }
    }

    @Test
    fun `An activity which runs on a bank holiday`() {
      val nomisPayRates = listOf(NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110))
      val nomisScheduleRules = listOf(
        NomisScheduleRule(startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), monday = true),
      )

      // Request to run on bank holidays
      val request = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules).copy(
        runsOnBankHoliday = true,
      )

      whenever(activityRepository.saveAndFlush(any())).thenReturn(buildDummyActivity())

      val response = service.migrateActivity(request)

      assertThat(response.activityId).isEqualTo(1)
      assertThat(response.splitRegimeActivityId).isNull()

      verify(activityRepository).saveAndFlush(activityCaptor.capture())

      with(activityCaptor.firstValue) {
        with(schedules().first()) {
          assertThat(slots()).hasSize(1)
          assertThat(runsOnBankHoliday).isTrue
        }
      }
    }

    private fun buildDummyActivity() = Activity(
      activityId = 1,
      prisonCode = "MDI",
      activityTier = getTier("Tier1"),
      activityCategory = getCategory("SAA_PRISON_JOBS")!!,
      summary = "Summary",
      description = "Description",
      startDate = LocalDate.now().minusDays(1),
      riskLevel = "Low",
      minimumIncentiveLevel = "Basic",
      minimumIncentiveNomisCode = "BAS",
      createdTime = LocalDateTime.now(),
      createdBy = "TEST",
    )

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
        minimumIncentiveLevel = "BAS",
        runsOnBankHoliday = false,
        scheduleRules,
        payRates,
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
      whenever(activityTierRepository.findAll()).thenReturn(listOfTiers)
    }

    @Test
    fun `All program services map to tier 1`() {
      assertThat(service.mapProgramToTier("IND_DTP")).isEqualTo(getTier("Tier1"))
      assertThat(service.mapProgramToTier("T2ICA")).isEqualTo(getTier("Tier1"))
      assertThat(service.mapProgramToTier("ANY")).isEqualTo(getTier("Tier1"))
      assertThat(service.mapProgramToTier("CLNR")).isEqualTo(getTier("Tier1"))
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
}
