package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory2
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleFemale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleOver21
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.runEveryDayOfWeek
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as ActivityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule as EligibilityRuleEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

class ActivityServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityTierRepository: ActivityTierRepository = mock()
  private val eligibilityRuleRepository: EligibilityRuleRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val bankHolidayService: BankHolidayService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val educationLevel = ReferenceCode(
    domain = "EDU_LEVEL",
    code = "1",
    description = "Reading Measure 1.0",
    parentCode = "STL",
    activeFlag = "Y",
    listSeq = 6,
    systemDataFlag = "N",
  )

  private val studyArea = ReferenceCode(
    domain = "STUDY_AREA",
    code = "ENGLA",
    description = "English Language",
    activeFlag = "Y",
    listSeq = 99,
    systemDataFlag = "N",
  )

  private val inactiveEducationLevel = ReferenceCode(
    domain = "EDU_LEVEL",
    code = "1",
    description = "Reading Measure 1.0",
    parentCode = "STL",
    activeFlag = "N",
    listSeq = 6,
    systemDataFlag = "N",
  )

  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  private val activityCaptor = argumentCaptor<ActivityEntity>()

  private fun service(
    daysInAdvance: Long = 7L,
  ) = ActivityService(
    activityRepository,
    activityCategoryRepository,
    activityTierRepository,
    eligibilityRuleRepository,
    activityScheduleRepository,
    prisonPayBandRepository,
    prisonApiClient,
    prisonRegimeService,
    bankHolidayService,
    daysInAdvance = daysInAdvance,
    telemetryClient = telemetryClient,
  )

  private val location = Location(
    locationId = 1,
    locationType = "type",
    description = "description",
    agencyId = "MDI",
  )

  private val caseLoad = "MDI"

  @BeforeEach
  fun setUp() {
    openMocks(this)
    whenever(prisonApiClient.getLocation(1)).thenReturn(Mono.just(location))
    whenever(prisonRegimeService.getPrisonRegimeByPrisonCode(any())).thenReturn(transform(prisonRegime()))
    whenever(prisonRegimeService.getPrisonTimeSlots(any())).thenReturn(
      transform(prisonRegime()).let { pr ->
        mapOf(
          TimeSlot.AM to Pair(pr.amStart, pr.amFinish),
          TimeSlot.PM to Pair(pr.pmStart, pr.pmFinish),
          TimeSlot.ED to Pair(pr.edStart, pr.edFinish),
        )
      },
    )
    addCaseloadIdToRequestHeader(caseLoad)
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `createActivity - success`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-1.json")
      .copy(startDate = TimeSource.tomorrow())

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier()))
    whenever(eligibilityRuleRepository.findById(eligibilityRuleOver21.eligibilityRuleId)).thenReturn(
      Optional.of(
        eligibilityRuleOver21,
      ),
    )
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))
    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity())

    service().createActivity(createActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)
    verify(eligibilityRuleRepository).findById(any())

    with(activityCaptor.firstValue) {
      assertThat(eligibilityRules()).hasSize(1)
      assertThat(activityPay()).hasSize(2)
      assertThat(activityMinimumEducationLevel()).hasSize(1)
      assertThat(activityCategory).isEqualTo(activityCategory())
      assertThat(activityTier).isEqualTo(activityTier())
    }
  }

  @Test
  fun `createActivity - success - creates activity with multi-week schedule`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-9.json")
      .copy(startDate = TimeSource.tomorrow())

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier()))
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))
    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity())

    service(daysInAdvance = 14).createActivity(createActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)

    with(activityCaptor.firstValue) {
      assertThat(activityPay()).hasSize(2)
      assertThat(activityMinimumEducationLevel()).hasSize(1)
      assertThat(activityCategory).isEqualTo(activityCategory())
      assertThat(activityTier).isEqualTo(activityTier())
      with(schedules().first()) {
        assertThat(slots()).hasSize(2)

        // Week one only has AM slots
        assertThat(
          instances().filter {
            getWeekNumber(it.sessionDate) == 1 && it.startTime == LocalTime.of(9, 0)
          },
        ).isNotEmpty
        assertThat(
          instances().filter {
            getWeekNumber(it.sessionDate) == 1 && it.startTime == LocalTime.of(13, 0)
          },
        ).isEmpty()

        // Week two only has PM slots
        assertThat(
          instances().filter {
            getWeekNumber(it.sessionDate) == 2 && it.startTime == LocalTime.of(13, 0)
          },
        ).isNotEmpty
        assertThat(
          instances().filter {
            getWeekNumber(it.sessionDate) == 2 && it.startTime == LocalTime.of(9, 0)
          },
        ).isEmpty()
      }
    }
  }

  @Test
  fun `createActivity - start date must be in the future`() {
    assertThatThrownBy {
      service().createActivity(
        activityCreateRequest().copy(startDate = TimeSource.today()),
        "SCH_ACTIVITY",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date must be in the future")
  }

  @Test
  fun `createActivity - duplicate`() {
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleFemale))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(activityRepository.existsActivityByPrisonCodeAndSummary(any(), any())).thenReturn(true)

    val createDuplicateActivityRequest = activityCreateRequest()

    assertThatThrownBy { service().createActivity(createDuplicateActivityRequest, "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Duplicate activity name detected for this prison (MDI): '${createDuplicateActivityRequest.summary}'")

    verify(activityRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createActivity - category id not found`() {
    val activityCreateRequest = activityCreateRequest()

    whenever(activityCategoryRepository.findById(activityCreateRequest.categoryId!!)).thenReturn(Optional.empty())

    assertThatThrownBy { service().createActivity(activityCreateRequest, "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Category ${activityCreateRequest.categoryId} not found")
  }

  @Test
  fun `createActivity - tier id not found`() {
    val activityCreateRequest = activityCreateRequest()

    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(activityCreateRequest.tierId!!)).thenReturn(Optional.empty())

    assertThatThrownBy { service().createActivity(activityCreateRequest(), "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Tier ${activityCreateRequest.tierId} not found")
  }

  @Test
  fun `createActivity - eligibility rule not found`() {
    val activityCreateRequest = activityCreateRequest()

    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(eligibilityRuleRepository.findById(activityCreateRequest.eligibilityRuleIds.first())).thenReturn(Optional.empty())

    assertThatThrownBy {
      service().createActivity(activityCreateRequest, "SCH_ACTIVITY")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Eligibility Rule ${activityCreateRequest.eligibilityRuleIds.first()} not found")
  }

  @Test
  fun `createActivity - fails to add schedule when prison do not match with the activity and location supplied`() {
    val createActivityRequest =
      activityCreateRequest(prisonCode = "DOES_NOT_MATCH", educationLevel = educationLevel, studyArea = studyArea)

    assertThatThrownBy { service().createActivity(createActivityRequest, "SCH_ACTIVITY") }
      .isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `getActivityById returns an activity for known activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity(prisonCode = caseLoad)))

    assertThat(service().getActivityById(1)).isInstanceOf(ModelActivity::class.java)
  }

  @Test
  fun `getActivityById throws a CaseLoadAccessException an activity with a different prison code`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity(prisonCode = "DIFFERENT_PRISON_CODE")))

    assertThatThrownBy { service().getActivityById(1) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `getActivityById throws entity not found exception for unknown activity ID`() {
    assertThatThrownBy { service().getActivityById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity -1 not found")
  }

  @Test
  fun `getActivitiesByCategoryInPrison returns list of activities`() {
    val category = activityCategory()

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(category))
    whenever(activityRepository.getAllByPrisonCodeAndActivityCategory("MDI", category))
      .thenReturn(listOf(activityEntity()))

    assertThat(
      service().getActivitiesByCategoryInPrison(
        "MDI",
        1,
      ),
    ).isEqualTo(listOf(activityEntity()).toModelLite())

    verify(activityRepository, times(1)).getAllByPrisonCodeAndActivityCategory("MDI", category)
  }

  @Test
  fun `getActivitiesByCategoryInPrison throws entity not found exception for unknown category ID`() {
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service().getActivitiesByCategoryInPrison("MDI", 1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Category 1 not found")
  }

  @Test
  fun `getActivitiesInPrison only returns list of live activities`() {
    whenever(activityRepository.getAllByPrisonCode("MDI"))
      .thenReturn(
        listOf(
          activityEntity(),
          activityEntity(startDate = LocalDate.of(2023, 1, 1), endDate = LocalDate.of(2023, 1, 2)),
        ),
      )

    assertThat(
      service().getActivitiesInPrison(
        "MDI",
        true,
      ),
    ).isEqualTo(listOf(activityEntity()).toModelLite())

    verify(activityRepository, times(1)).getAllByPrisonCode("MDI")
  }

  @Test
  fun `getActivitiesInPrison returns all activities including archived activities`() {
    whenever(activityRepository.getAllByPrisonCode("MDI"))
      .thenReturn(
        listOf(
          activityEntity(),
          activityEntity(startDate = LocalDate.of(2023, 1, 1), endDate = LocalDate.of(2023, 1, 2)),
        ),
      )

    assertThat(
      service().getActivitiesInPrison(
        "MDI",
        false,
      ),
    ).isEqualTo(
      listOf(
        activityEntity(),
        activityEntity(startDate = LocalDate.of(2023, 1, 1), endDate = LocalDate.of(2023, 1, 2)),
      ).toModelLite(),
    )

    verify(activityRepository, times(1)).getAllByPrisonCode("MDI")
  }

  @Test
  fun `getSchedulesForActivity returns list of schedules`() {
    val activity = activityEntity()

    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activity))
    whenever(activityScheduleRepository.getAllByActivity(activity))
      .thenReturn(listOf(activitySchedule(activityEntity())))

    assertThat(service().getSchedulesForActivity(1)).isEqualTo(listOf(activitySchedule(activityEntity())).toModelLite())

    verify(activityScheduleRepository, times(1)).getAllByActivity(activity)
  }

  @Test
  fun `getSchedulesForActivity throws entity not found exception for unknown activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service().getSchedulesForActivity(1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity 1 not found")
  }

  @Test
  fun `createActivity - education level description does not match NOMIS`() {
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleOver21))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel.copy(description = "Reading Measure 1.0")))

    assertThatThrownBy {
      service().createActivity(
        activityCreateRequest(educationLevel = educationLevel.copy(description = "Reading Measure 2.0")),
        "SCH_ACTIVITY",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The education level description 'Reading Measure 2.0' does not match the NOMIS education level 'Reading Measure 1.0'")
  }

  @Test
  fun `createActivity - education level is not active in NOMIS`() {
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleFemale))
    whenever(prisonApiClient.getEducationLevel(inactiveEducationLevel.code)).thenReturn(Mono.just(inactiveEducationLevel))

    assertThatThrownBy {
      service().createActivity(activityCreateRequest(educationLevel = inactiveEducationLevel), "SCH_ACTIVITY")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The education level code '1' is not active in NOMIS")
  }

  @Test
  fun `createActivity - study area description does not match NOMIS`() {
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleFemale))
    whenever(prisonApiClient.getEducationLevel(any())).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea(studyArea.code)).thenReturn(Mono.just(studyArea.copy(description = "DOES NOT MATCH")))

    assertThatThrownBy {
      service().createActivity(
        activityCreateRequest(educationLevel = educationLevel, studyArea = studyArea),
        "SCH_ACTIVITY",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The study area description 'English Language' does not match the NOMIS study area 'DOES NOT MATCH'")
  }

  @Test
  fun `createActivity - study area is not active in NOMIS`() {
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleFemale))
    whenever(prisonApiClient.getEducationLevel(any())).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea(studyArea.code)).thenReturn(Mono.just(studyArea.copy(activeFlag = "N")))

    assertThatThrownBy {
      service().createActivity(
        activityCreateRequest(educationLevel = educationLevel, studyArea = studyArea),
        "SCH_ACTIVITY",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The study area code 'ENGLA' is not active in NOMIS")
  }

  @Test
  fun `createActivity - Create In-cell activity`() {
    val createdBy = "SCH_ACTIVITY"

    val createInCellActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-6.json")
      .copy(startDate = TimeSource.tomorrow())

    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))

    whenever(activityRepository.saveAndFlush(any())).thenReturn(savedActivityEntity)

    service().createActivity(createInCellActivityRequest, createdBy)

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(inCell).isTrue

      with(schedules()[0]) {
        assertThat(internalLocationId).isNull()
        assertThat(internalLocationCode).isNull()
        assertThat(internalLocationDescription).isNull()
      }
    }
  }

  @Test
  fun `updateActivity - success`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier()))

    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(savedActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    service().updateActivity(moorlandPrisonCode, 1, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)
    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(1)
        assertThat(code).isEqualTo("category code")
        assertThat(description).isEqualTo("category description")
      }
      with(activityTier!!) {
        assertThat(activityTierId).isEqualTo(1)
        assertThat(code).isEqualTo("T1")
        assertThat(description).isEqualTo("Tier 1")
      }
    }
  }

  @Test
  fun `updateActivity - duplicate summary`() {
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)
    whenever(activityRepository.existsActivityByPrisonCodeAndSummary(any(), any())).thenReturn(true)

    val updateDuplicateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-5.json")

    assertThatThrownBy {
      service().updateActivity(
        moorlandPrisonCode,
        1,
        updateDuplicateActivityRequest,
        "SCH_ACTIVITY",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Duplicate activity name detected for this prison (MDI): 'IT level 2'")

    verify(activityRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateActivity - category id not found`() {
    val updatedBy = "SCH_ACTIVITY"
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service().updateActivity(moorlandPrisonCode, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Category 1 not found")
  }

  @Test
  fun `updateActivity - tier id not found`() {
    val updatedBy = "SCH_ACTIVITY"
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service().updateActivity(moorlandPrisonCode, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Tier 1 not found")
  }

  @Test
  fun `updateActivity - update category`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-2.json")

    val beforeActivityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(beforeActivityCategory))

    val afterActivityCategory = activityCategory2()
    whenever(activityCategoryRepository.findById(2)).thenReturn(Optional.of(afterActivityCategory))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier()))

    val beforeActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(beforeActivityEntity)

    val afterActivityEntity: ActivityEntity = mapper.read("activity/updated-activity-entity-1.json")

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(moorlandPrisonCode, 1, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityCategoryRepository).findById(2)
    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue.activityCategory) {
      assertThat(activityCategoryId).isEqualTo(2)
      assertThat(code).isEqualTo("category code 2")
      assertThat(description).isEqualTo("category description 2")
    }
  }

  @Test
  fun `updateActivity - update end date`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-4.json")
    val beforeActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-3.json")

    beforeActivityEntity.addSchedule(
      description = "Woodwork",
      internalLocation = Location(
        locationId = 1,
        internalLocationCode = "WW",
        description = "The wood work room description",
        locationType = "APP",
        agencyId = "MDI",
      ),
      capacity = 10,
      startDate = beforeActivityEntity.startDate,
      endDate = beforeActivityEntity.endDate,
      runsOnBankHoliday = true,
      scheduleWeeks = 1,
    )

    with(beforeActivityEntity.schedules().first()) {
      allocatePrisoner(
        prisonerNumber = "123456".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 10001,
        allocatedBy = "FRED",
        endDate = updateActivityRequest.endDate?.plusYears(1),
      )

      allocatePrisoner(
        prisonerNumber = "654321".toPrisonerNumber(),
        payBand = lowPayBand,
        bookingId = 20002,
        allocatedBy = "BOB",
        endDate = null,
      )
    }

    val afterActivityEntity: ActivityEntity = mapper.read("activity/updated-activity-entity-1.json")

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(beforeActivityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)

    service().updateActivity(moorlandPrisonCode, 1, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(endDate).isEqualTo("2023-12-31")
      assertThat(schedules().first().endDate).isEqualTo("2023-12-31")
      assertThat(schedules().first().allocations().find { it.prisonerNumber == "123456" }?.endDate).isEqualTo("2023-12-31")
      assertThat(schedules().first().allocations().find { it.prisonerNumber == "654321" }?.endDate).isNull()
    }
  }

  @Test
  fun `updateActivity - update pay`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-3.json")
    val activityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        17,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh())

    service().updateActivity(moorlandPrisonCode, 17, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(activityPay()).hasSize(1)
    }
  }

  @Test
  fun `updateActivity - update start date fails if new date not in future`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(startDate = TimeSource.today()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Start date must be in the future.")
  }

  @Test
  fun `updateActivity - update start date fails if new date after end date`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(
        moorlandPrisonCode,
        1,
        ActivityUpdateRequest(startDate = activity.endDate?.plusDays(1)),
        "TEST",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Start date cannot be after the end date.")
  }

  @Test
  fun `updateActivity - update start date fails if activity has already started`() {
    val activity = activityEntity(startDate = TimeSource.today(), endDate = TimeSource.tomorrow().plusWeeks(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(startDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Activity already started.")
  }

  @Test
  fun `updateActivity - update start date fails if activity has allocations already started`() {
    val activity = activityEntity(startDate = TimeSource.today(), endDate = TimeSource.tomorrow().plusWeeks(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(startDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Activity already started.")
  }

  @Test
  fun `updateActivity - update end date fails if removeEndDate is also true`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(
        moorlandPrisonCode,
        1,
        ActivityUpdateRequest(endDate = activity.endDate?.plusDays(1), removeEndDate = true),
        "TEST",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("removeEndDate flag cannot be true when an endDate is also supplied.")
  }

  @Test
  fun `updateActivity - removal of the end date is successful`() {
    val activity = activityEntity(
      startDate = TimeSource.tomorrow(),
      endDate = TimeSource.tomorrow().plusDays(1),
      noSchedules = true,
    ).apply {
      this.addSchedule(
        activitySchedule(
          this,
          activityScheduleId = 1,
          daysOfWeek = DayOfWeek.values().toSet(),
        ),
      )
    }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThat(activity.endDate).isEqualTo(TimeSource.tomorrow().plusDays(1))
    activity.schedules().forEach { activitySchedule ->
      assertThat(
        activitySchedule.instances().find { i -> i.sessionDate > TimeSource.tomorrow().plusDays(1) },
      ).isNull()
    }

    service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(removeEndDate = true), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    with(activityCaptor.firstValue) {
      assertThat(endDate).isNull()
      schedules().forEach { s ->
        // assert end date set to null and instances are scheduled into the future
        assertThat(s.endDate).isNull()
        assertThat(s.instances().find { i -> i.sessionDate > TimeSource.tomorrow().plusDays(1) }).isNotNull
      }
    }
  }

  @Test
  fun `updateActivity - setting of the end date is successful`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    activity.schedules().forEach {
      val everydaySlot = it.addSlot(1, LocalTime.NOON, LocalTime.NOON.plusHours(1), DayOfWeek.values().toSet())
      it.addInstance(TimeSource.tomorrow().plusDays(1), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(2), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(3), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(4), everydaySlot)
    }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(endDate = TimeSource.tomorrow()), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    with(activityCaptor.firstValue) {
      // assert end date set and future instances ahead of the new end date are removed
      assertThat(endDate).isEqualTo(TimeSource.tomorrow())
      schedules().forEach { s ->
        assertThat(s.endDate).isEqualTo(TimeSource.tomorrow())
        assertThat(s.instances().find { i -> i.sessionDate > TimeSource.tomorrow() }).isNull()
        assertThat(s.instances().find { i -> i.sessionDate == TimeSource.tomorrow() }).isNotNull
      }
    }
  }

  @Test
  fun `updateActivity - prolonging the end date into the future is successful`() {
    val activity =
      activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1)).also { activity ->
        activity.schedules().forEach { schedule -> schedule.slots().onEach { slot -> slot.runEveryDayOfWeek() } }
      }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    val newEndDate = activity.endDate!!.plusDays(4)

    service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(endDate = newEndDate), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    with(activityCaptor.firstValue) {
      assertThat(endDate).isEqualTo(TimeSource.tomorrow().plusDays(5))
      schedules().forEach { s ->
        // assert end date set and future instances ahead of the new end date are created
        assertThat(s.endDate).isEqualTo(newEndDate)
        assertThat(s.instances().find { i -> i.sessionDate == newEndDate }).isNotNull
        assertThat(s.instances().find { i -> i.sessionDate > newEndDate.plusDays(1) }).isNull()
      }
    }
  }

  @Test
  fun `updateActivity - bringing the end date closer is successful`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(5))
    activity.schedules().forEach {
      val everydaySlot = it.addSlot(1, LocalTime.NOON, LocalTime.NOON.plusHours(1), DayOfWeek.values().toSet())
      it.addInstance(TimeSource.tomorrow().plusDays(1), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(2), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(3), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(4), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(5), everydaySlot)
    }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(
      moorlandPrisonCode,
      1,
      ActivityUpdateRequest(endDate = TimeSource.tomorrow().plusDays(1)),
      "TEST",
    )

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    with(activityCaptor.firstValue) {
      // assert end date set and future instances ahead of the new end date are removed
      assertThat(endDate).isEqualTo(TimeSource.tomorrow().plusDays(1))
      schedules().forEach { s ->
        assertThat(s.endDate).isEqualTo(TimeSource.tomorrow().plusDays(1))
        assertThat(s.instances().find { i -> i.sessionDate == TimeSource.tomorrow().plusDays(1) }).isNotNull
        assertThat(s.instances().find { i -> i.sessionDate > TimeSource.tomorrow().plusDays(1) }).isNull()
      }
    }
  }

  @Test
  fun `updateActivity - fails if activity has already ended (archived)`() {
    val activity = activityEntity(
      startDate = TimeSource.yesterday().minusDays(1),
      endDate = TimeSource.yesterday(),
    )

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        moorlandPrisonCode,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(moorlandPrisonCode, 1, ActivityUpdateRequest(endDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity cannot be updated because it is now archived.")
  }

  @Test
  fun `updateActivity - fails if activity not found`() {
    whenever(activityRepository.findByActivityIdAndPrisonCode(1, moorlandPrisonCode)).thenReturn(null)

    assertThatThrownBy {
      service().updateActivity(pentonvillePrisonCode, 1, ActivityUpdateRequest(endDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `updateActivity - add new slot (adds new instances)`() {
    val activity = activityEntity(noSchedules = true).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val schedule = activity.addSchedule(activitySchedule(activity, noInstances = true, noSlots = true))

    schedule.slots() hasSize 0
    schedule.instances() hasSize 0

    val tomorrow = LocalDate.now().plusDays(1)

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "AM",
            monday = tomorrow.dayOfWeek == DayOfWeek.MONDAY,
            tuesday = tomorrow.dayOfWeek == DayOfWeek.TUESDAY,
            wednesday = tomorrow.dayOfWeek == DayOfWeek.WEDNESDAY,
            thursday = tomorrow.dayOfWeek == DayOfWeek.THURSDAY,
            friday = tomorrow.dayOfWeek == DayOfWeek.FRIDAY,
            saturday = tomorrow.dayOfWeek == DayOfWeek.SATURDAY,
            sunday = tomorrow.dayOfWeek == DayOfWeek.SUNDAY,
          ),
        ),
      ),
      "TEST",
    )

    schedule.slots() hasSize 1
    schedule.instances() hasSize 1
  }

  @Test
  fun `updateActivity - remove slots (removes instances)`() {
    val activity = activityEntity(noSchedules = true).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val tomorrow = LocalDate.now().plusDays(1)

    val schedule = activity.addSchedule(activitySchedule(activity, noInstances = true, noSlots = true))
    val slot1 = schedule.addSlot(
      1,
      prisonRegime().amStart,
      prisonRegime().amFinish,
      setOf(
        tomorrow.dayOfWeek,
      ),
    )
    val slot2 = schedule.addSlot(
      1,
      prisonRegime().pmStart,
      prisonRegime().pmFinish,
      setOf(tomorrow.plusDays(1).dayOfWeek),
    )
    val instance1 = schedule.addInstance(tomorrow, slot1)
    val instance2 = schedule.addInstance(tomorrow.plusDays(1), slot2)

    schedule.slots() hasSize 2
    schedule.instances() hasSize 2
    assertThat(schedule.slots()).containsAll(listOf(slot1, slot2))
    assertThat(schedule.instances()).containsAll(listOf(instance1, instance2))

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "AM",
            monday = tomorrow.dayOfWeek == DayOfWeek.MONDAY,
            tuesday = tomorrow.dayOfWeek == DayOfWeek.TUESDAY,
            wednesday = tomorrow.dayOfWeek == DayOfWeek.WEDNESDAY,
            thursday = tomorrow.dayOfWeek == DayOfWeek.THURSDAY,
            friday = tomorrow.dayOfWeek == DayOfWeek.FRIDAY,
            saturday = tomorrow.dayOfWeek == DayOfWeek.SATURDAY,
            sunday = tomorrow.dayOfWeek == DayOfWeek.SUNDAY,
          ),
        ),
      ),
      "TEST",
    )

    schedule.slots() hasSize 1
    schedule.instances() hasSize 1
    assertThat(schedule.slots()).contains(slot1)
    assertThat(schedule.instances()).contains(instance1)
  }

  @Test
  fun `updateActivity - update existing slot (adds & removes instances)`() {
    val activity = activityEntity(noSchedules = true).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val tomorrow = LocalDate.now().plusDays(1)

    val schedule = activity.addSchedule(activitySchedule(activity, noInstances = true, noSlots = true))

    val slot1 = schedule.addSlot(
      1,
      prisonRegime().amStart,
      prisonRegime().amFinish,
      setOf(tomorrow.dayOfWeek),
    )
    val slot2 = schedule.addSlot(
      1,
      prisonRegime().pmStart,
      prisonRegime().pmFinish,
      setOf(tomorrow.plusDays(1).dayOfWeek),
    )
    val instance1 = schedule.addInstance(tomorrow, slot1)
    val instance2 = schedule.addInstance(tomorrow.plusDays(1), slot2)

    assertThat(schedule.slots()).containsAll(listOf(slot1, slot2))
    assertThat(schedule.instances()).containsAll(listOf(instance1, instance2))

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "ED",
            monday = tomorrow.dayOfWeek == DayOfWeek.MONDAY,
            tuesday = tomorrow.dayOfWeek == DayOfWeek.TUESDAY,
            wednesday = tomorrow.dayOfWeek == DayOfWeek.WEDNESDAY,
            thursday = tomorrow.dayOfWeek == DayOfWeek.THURSDAY,
            friday = tomorrow.dayOfWeek == DayOfWeek.FRIDAY,
            saturday = tomorrow.dayOfWeek == DayOfWeek.SATURDAY,
            sunday = tomorrow.dayOfWeek == DayOfWeek.SUNDAY,
          ),
          Slot(
            weekNumber = 1,
            timeSlot = "PM",
            monday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.MONDAY,
            tuesday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.TUESDAY,
            wednesday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.WEDNESDAY,
            thursday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.THURSDAY,
            friday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.FRIDAY,
            saturday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.SATURDAY,
            sunday = tomorrow.plusDays(1).dayOfWeek == DayOfWeek.SUNDAY,
          ),
        ),
      ),
      "TEST",
    )

    // One slot and instance should be removed and replaced with one new slot and instance
    schedule.slots() hasSize 2
    assertThat(schedule.slots()).doesNotContain(slot1)
    assertThat(schedule.slots()).contains(slot2)
    schedule.instances() hasSize 2
    assertThat(schedule.instances()).doesNotContain(instance1)
    assertThat(schedule.instances()).contains(instance2)
  }

  @Test
  fun `updateActivity - now runs on bank holiday`() {
    val bankHoliday = TimeSource.tomorrow().also { whenever(bankHolidayService.isEnglishBankHoliday(it)) doReturn true }
    val dayAfterBankHoliday = bankHoliday.plusDays(1)

    val activity = activityEntity(startDate = bankHoliday, noSchedules = true).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val schedule =
      activity.addSchedule(activitySchedule(activity, runsOnBankHolidays = false, noInstances = true, noSlots = true))
        .apply {
          addSlot(
            1,
            LocalTime.NOON,
            LocalTime.NOON.plusHours(1),
            setOf(bankHoliday.dayOfWeek, dayAfterBankHoliday.dayOfWeek),
          )
        }

    schedule.instances() hasSize 0

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(runsOnBankHoliday = true),
      updatedBy = "TEST",
    )

    schedule.instances() hasSize 2
    schedule.instances()[0].sessionDate isEqualTo bankHoliday
    schedule.instances()[1].sessionDate isEqualTo dayAfterBankHoliday
  }

  @Test
  fun `updateActivity - no longer runs on bank holiday`() {
    val bankHoliday = TimeSource.tomorrow().also { whenever(bankHolidayService.isEnglishBankHoliday(it)) doReturn true }
    val dayAfterBankHoliday = bankHoliday.plusDays(1)

    val activity = activityEntity(startDate = bankHoliday, noSchedules = true).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val schedule =
      activity.addSchedule(activitySchedule(activity, runsOnBankHolidays = false, noInstances = true, noSlots = true))
        .apply {
          addSlot(
            1,
            LocalTime.NOON,
            LocalTime.NOON.plusHours(1),
            setOf(bankHoliday.dayOfWeek, dayAfterBankHoliday.dayOfWeek),
          )
        }

    schedule.instances() hasSize 0

    // Set up bank holiday first so it can be removed
    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(runsOnBankHoliday = true),
      updatedBy = "TEST",
    )
    assertThat(schedule.instances()).hasSize(2)

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(runsOnBankHoliday = false),
      updatedBy = "TEST",
    )

    schedule.instances() hasSize 1
    schedule.instances().first().sessionDate isEqualTo dayAfterBankHoliday
  }

  @Test
  fun `updateActivity - update start date of multi-week schedule`() {
    val tomorrow = LocalDate.now().plusDays(1)

    val activity = activityEntity(
      startDate = tomorrow.plusDays(14),
      noSchedules = true,
    ).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val schedule = activity.addSchedule(
      activitySchedule(activity, scheduleWeeks = 2, noSlots = true, noInstances = true, noAllocations = true),
    ).apply {
      addSlot(
        weekNumber = 1,
        startTime = LocalTime.NOON,
        endTime = LocalTime.NOON.plusHours(1),
        daysOfWeek = setOf(tomorrow.dayOfWeek, tomorrow.plusDays(2).dayOfWeek),
      )
      addSlot(
        weekNumber = 2,
        startTime = LocalTime.NOON,
        endTime = LocalTime.NOON.plusHours(1),
        daysOfWeek = setOf(tomorrow.plusDays(1).dayOfWeek, tomorrow.plusDays(3).dayOfWeek),
      )
    }

    schedule.instances() hasSize 0

    val daysToSchedule = 14L

    service(daysInAdvance = daysToSchedule).updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(startDate = tomorrow),
      updatedBy = "TEST",
    )

    val week1Instances = schedule.instances().filter { schedule.getWeekNumber(it.sessionDate) == 1 }
    assertThat(week1Instances.size).isEqualTo(2)
    week1Instances.all {
      listOf(
        tomorrow.dayOfWeek,
        tomorrow.plusDays(2).dayOfWeek,
      ).contains(it.dayOfWeek())
    }

    val week2Instances = schedule.instances().filter { schedule.getWeekNumber(it.sessionDate) == 2 }
    assertThat(week2Instances.size).isEqualTo(2)
    week2Instances.all {
      listOf(
        tomorrow.plusDays(1).dayOfWeek,
        tomorrow.plusDays(3).dayOfWeek,
      ).contains(it.dayOfWeek())
    }
  }

  @Test
  fun `updateActivity - update slots of multi-week schedule`() {
    val tomorrow = LocalDate.now().plusDays(1)

    val activity = activityEntity(
      startDate = tomorrow,
      noSchedules = true,
    ).also {
      whenever(
        activityRepository.findByActivityIdAndPrisonCodeWithFilters(
          it.activityId,
          it.prisonCode,
          LocalDate.now(),
        ),
      ) doReturn (it)
    }

    val schedule = activity.addSchedule(
      activitySchedule(activity, scheduleWeeks = 2, noSlots = true, noInstances = true, noAllocations = true),
    ).apply {
      addSlot(
        weekNumber = 1,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
    }

    schedule.instances() hasSize 0

    val week2SlotDay = tomorrow.plusDays(1).dayOfWeek

    val daysToSchedule = 14L
    service(daysInAdvance = daysToSchedule).updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "AM",
            monday = tomorrow.dayOfWeek == DayOfWeek.MONDAY,
            tuesday = tomorrow.dayOfWeek == DayOfWeek.TUESDAY,
            wednesday = tomorrow.dayOfWeek == DayOfWeek.WEDNESDAY,
            thursday = tomorrow.dayOfWeek == DayOfWeek.THURSDAY,
            friday = tomorrow.dayOfWeek == DayOfWeek.FRIDAY,
            saturday = tomorrow.dayOfWeek == DayOfWeek.SATURDAY,
            sunday = tomorrow.dayOfWeek == DayOfWeek.SUNDAY,
          ),
          Slot(
            weekNumber = 2,
            timeSlot = "AM",
            monday = week2SlotDay == DayOfWeek.MONDAY,
            tuesday = week2SlotDay == DayOfWeek.TUESDAY,
            wednesday = week2SlotDay == DayOfWeek.WEDNESDAY,
            thursday = week2SlotDay == DayOfWeek.THURSDAY,
            friday = week2SlotDay == DayOfWeek.FRIDAY,
            saturday = week2SlotDay == DayOfWeek.SATURDAY,
            sunday = week2SlotDay == DayOfWeek.SUNDAY,
          ),
        ),
      ),
      updatedBy = "TEST",
    )

    val week1Instances = schedule.instances().filter { schedule.getWeekNumber(it.sessionDate) == 1 }
    assertThat(week1Instances.size).isEqualTo(1)
    week1Instances.all {
      listOf(tomorrow.dayOfWeek).contains(it.dayOfWeek())
    }

    val week2Schedules = schedule.instances().filter { schedule.getWeekNumber(it.sessionDate) == 2 }
    assertThat(week2Schedules.size).isEqualTo(1)
    week2Schedules.all {
      listOf(tomorrow.plusDays(1).dayOfWeek).contains(it.dayOfWeek())
    }
  }

  @Test
  fun `addScheduleInstances - should add new schedule instances where required`() {
    val tomorrow = LocalDate.now().plusDays(1)

    val activity = activityEntity(
      startDate = tomorrow,
      noSchedules = true,
    )

    val schedule = activity.addSchedule(
      activitySchedule(activity, scheduleWeeks = 2, noSlots = true, noInstances = true, noAllocations = true),
    ).apply {
      addSlot(
        weekNumber = 1,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
      addSlot(
        weekNumber = 2,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
    }

    assertThat(schedule.instances()).isEmpty()

    service(7).addScheduleInstances(schedule)

    schedule.instances() hasSize 1

    service(14).addScheduleInstances(schedule)

    schedule.instances() hasSize 2
  }
}
