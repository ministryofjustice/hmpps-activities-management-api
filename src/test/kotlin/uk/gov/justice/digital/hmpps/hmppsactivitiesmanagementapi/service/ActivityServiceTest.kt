package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory2
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleFemale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
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

  val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  @Captor
  private lateinit var activityEntityCaptor: ArgumentCaptor<ActivityEntity>

  private val service = ActivityService(
    activityRepository,
    activityCategoryRepository,
    activityTierRepository,
    eligibilityRuleRepository,
    activityScheduleRepository,
    prisonPayBandRepository,
    prisonApiClient,
    prisonRegimeService,
    bankHolidayService,
  )
  private val location = Location(
    locationId = 1,
    locationType = "type",
    description = "description",
    agencyId = "MDI",
  )

  @BeforeEach
  fun setUp() {
    openMocks(this)
    whenever(prisonApiClient.getLocation(1)).thenReturn(Mono.just(location))
    whenever(prisonRegimeService.getPrisonRegimeByPrisonCode("MDI")).thenReturn(transform(prisonRegime()))
  }

  @Test
  fun `createActivity - success`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))

    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))

    val savedActivityEntity: ActivityEntity = mapper.read("/activity/activity-entity-1.json")

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))
    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(savedActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    service.createActivity(createActivityRequest, createdBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)
    verify(eligibilityRuleRepository).findById(any())
    verify(activityRepository).saveAndFlush(activityArg)

    with(activityArg) {
      assertThat(eligibilityRules()).hasSize(1)
      assertThat(activityPay()).hasSize(2)
      assertThat(activityMinimumEducationLevel()).hasSize(1)
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(1)
        assertThat(code).isEqualTo("category code")
        assertThat(description).isEqualTo("category description")
      }
      with(activityTier) {
        assertThat(activityTierId).isEqualTo(1)
        assertThat(code).isEqualTo("T1")
        assertThat(description).isEqualTo("Tier 1")
      }
    }
  }

  @Test
  fun `createActivity - duplicate`() {
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleFemale))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(activityRepository.existsActivityByPrisonCodeAndSummary(any(), any())).thenReturn(true)

    val createDuplicateActivityRequest: ActivityCreateRequest = mock {
      on { prisonCode } doReturn (moorlandPrisonCode)
      on { summary } doReturn ("IT level 1")
    }

    assertThatThrownBy { service.createActivity(createDuplicateActivityRequest, "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Duplicate activity name detected for this prison (MDI): 'IT level 1'")

    verify(activityRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `createActivity - category id not found`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-1.json")

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Category 1 not found")
  }

  @Test
  fun `createActivity - tier id not found`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Tier 1 not found")
  }

  @Test
  fun `createActivity - eligibility rule not found`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Eligibility Rule 1 not found")
  }

  @Test
  fun `createActivity - fails to add schedule when prison do not match with the activity and location supplied`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-1.json")
      .copy(prisonCode = "DOES_NOT_MATCH")

    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(activityTierRepository.findById(any())).thenReturn(Optional.of(activityTier()))
    whenever(eligibilityRuleRepository.findById(any())).thenReturn(Optional.of(eligibilityRuleFemale))
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    assertThatThrownBy {
      service.createActivity(createActivityRequest, createdBy)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `getActivityById returns an activity for known activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity()))

    assertThat(service.getActivityById(1)).isInstanceOf(ModelActivity::class.java)
  }

  @Test
  fun `getActivityById throws entity not found exception for unknown activity ID`() {
    assertThatThrownBy { service.getActivityById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity -1 not found")
  }

  @Test
  fun `getActivitiesByCategoryInPrison returns list of activities`() {
    val category = activityCategory()

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(category))
    whenever(activityRepository.getAllByPrisonCodeAndActivityCategory("MDI", category))
      .thenReturn(listOf(activityEntity()))

    assertThat(
      service.getActivitiesByCategoryInPrison(
        "MDI",
        1,
      ),
    ).isEqualTo(listOf(activityEntity()).toModelLite())

    verify(activityRepository, times(1)).getAllByPrisonCodeAndActivityCategory("MDI", category)
  }

  @Test
  fun `getActivitiesByCategoryInPrison throws entity not found exception for unknown category ID`() {
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getActivitiesByCategoryInPrison("MDI", 1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Category 1 not found")
  }

  @Test
  fun `getActivitiesInPrison only returns list of active activities`() {
    whenever(activityRepository.getAllByPrisonCode("MDI"))
      .thenReturn(listOf(activityEntity(), activityEntity(startDate = LocalDate.of(2023, 1, 1), endDate = LocalDate.of(2023, 1, 2))))

    assertThat(
      service.getActivitiesInPrison(
        "MDI",
      ),
    ).isEqualTo(listOf(activityEntity()).toModelLite())

    verify(activityRepository, times(1)).getAllByPrisonCode("MDI")
  }

  @Test
  fun `getSchedulesForActivity returns list of schedules`() {
    val activity = activityEntity()

    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activity))
    whenever(activityScheduleRepository.getAllByActivity(activity))
      .thenReturn(listOf(activitySchedule(activityEntity())))

    assertThat(service.getSchedulesForActivity(1)).isEqualTo(listOf(activitySchedule(activityEntity())).toModelLite())

    verify(activityScheduleRepository, times(1)).getAllByActivity(activity)
  }

  @Test
  fun `getSchedulesForActivity throws entity not found exception for unknown activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getSchedulesForActivity(1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity 1 not found")
  }

  @Test
  fun `createActivity - education level description does not match NOMIS`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-4.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The education level description 'Reading Measure 2.0' does not match that of the NOMIS education level 'Reading Measure 1.0'")
  }

  @Test
  fun `createActivity - education level is not active in NOMIS`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-5.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(inactiveEducationLevel))

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The education level code '1' is not active in NOMIS")
  }

  @Test
  fun `createActivity - Create In-cell activity`() {
    val createdBy = "SCH_ACTIVITY"

    val createInCellActivityRequest: ActivityCreateRequest = mapper.read("activity/activity-create-request-6.json")

    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))

    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(savedActivityEntity)

    service.createActivity(createInCellActivityRequest, createdBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    with(activityArg) {
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
    val updatedBy = "SCH_ACTIVITY"

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))

    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))

    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(activityRepository.findById(1)).thenReturn(Optional.of(savedActivityEntity))

    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(savedActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    service.updateActivity(moorlandPrisonCode, 1, updateActivityRequest, updatedBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)
    verify(activityRepository).saveAndFlush(activityArg)

    with(activityArg) {
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(1)
        assertThat(code).isEqualTo("category code")
        assertThat(description).isEqualTo("category description")
      }
      with(activityTier) {
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
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(savedActivityEntity))
    whenever(activityRepository.existsActivityByPrisonCodeAndSummary(any(), any())).thenReturn(true)

    val updateDuplicateActivityRequest: ActivityUpdateRequest = mock {
      on { summary } doReturn ("IT level 1")
    }

    assertThatThrownBy { service.updateActivity(moorlandPrisonCode, 1, updateDuplicateActivityRequest, "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Duplicate activity name detected for this prison (MDI): 'IT level 1'")

    verify(activityRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `updateActivity - category id not found`() {
    val updatedBy = "SCH_ACTIVITY"
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(savedActivityEntity))

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.updateActivity(pentonvillePrisonCode, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Category 1 not found")
  }

  @Test
  fun `updateActivity - tier id not found`() {
    val updatedBy = "SCH_ACTIVITY"
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(savedActivityEntity))

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.updateActivity(pentonvillePrisonCode, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Tier 1 not found")
  }

  @Test
  fun `updateActivity - update category`() {
    val updatedBy = "SCH_ACTIVITY"

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-2.json")

    val beforeActivityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(beforeActivityCategory))

    val afterActivityCategory = activityCategory2()
    whenever(activityCategoryRepository.findById(2)).thenReturn(Optional.of(afterActivityCategory))

    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))

    val beforeActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(activityRepository.findById(1)).thenReturn(Optional.of(beforeActivityEntity))

    val afterActivityEntity: ActivityEntity = mapper.read("activity/updated-activity-entity-1.json")

    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service.updateActivity(moorlandPrisonCode, 1, updateActivityRequest, updatedBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    verify(activityCategoryRepository).findById(2)
    verify(activityRepository).saveAndFlush(activityArg)

    with(activityArg) {
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(2)
        assertThat(code).isEqualTo("category code 2")
        assertThat(description).isEqualTo("category description 2")
      }
      with(activityTier) {
        assertThat(activityTierId).isEqualTo(1)
        assertThat(code).isEqualTo("T1")
        assertThat(description).isEqualTo("Tier 1")
      }
    }
  }

  @Test
  fun `updateActivity - update end date`() {
    val updatedBy = "SCH_ACTIVITY"

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-4.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))

    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))

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
    )

    beforeActivityEntity.schedules().first().allocatePrisoner(
      prisonerNumber = "123456".toPrisonerNumber(),
      payBand = lowPayBand,
      bookingId = 10001,
      allocatedBy = "FRED",
    )

    whenever(activityRepository.findById(1)).thenReturn(Optional.of(beforeActivityEntity))

    val afterActivityEntity: ActivityEntity = mapper.read("activity/updated-activity-entity-1.json")

    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    service.updateActivity(moorlandPrisonCode, 1, updateActivityRequest, updatedBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    verify(activityRepository).saveAndFlush(activityArg)

    with(activityArg) {
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(1)
        assertThat(code).isEqualTo("category code")
        assertThat(description).isEqualTo("category description")
      }
      with(activityTier) {
        assertThat(activityTierId).isEqualTo(1)
        assertThat(code).isEqualTo("T1")
        assertThat(description).isEqualTo("Tier 1")
      }
      assertThat(endDate).isEqualTo("2023-12-31")
      assertThat(schedules().first().endDate).isEqualTo("2023-12-31")
      assertThat(schedules().first().allocations().first().endDate).isEqualTo("2023-12-31")
    }
  }

  @Test
  fun `updateActivity - update pay`() {
    val updatedBy = "SCH_ACTIVITY"

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-3.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))

    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))

    val activityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(activityRepository.findById(17)).thenReturn(Optional.of(activityEntity))

    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(activityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 0))
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    service.updateActivity(moorlandPrisonCode, 17, updateActivityRequest, updatedBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    verify(activityRepository).saveAndFlush(activityArg)

    with(activityArg) {
      assertThat(activityPay()).hasSize(1)
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(1)
        assertThat(code).isEqualTo("category code")
        assertThat(description).isEqualTo("category description")
      }
      with(activityTier) {
        assertThat(activityTierId).isEqualTo(1)
        assertThat(code).isEqualTo("T1")
        assertThat(description).isEqualTo("Tier 1")
      }
    }
  }
}
