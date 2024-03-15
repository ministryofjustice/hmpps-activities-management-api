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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory2
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleFemale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eligibilityRuleOver21
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.foundationTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.notInWorkCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.runEveryDayOfWeek
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityPayCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivitySummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITY_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_ORGANISER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.activityMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as ActivityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule as EligibilityRuleEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

class ActivityServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activitySummaryRepository: ActivitySummaryRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val eventTierRepository: EventTierRepository = mock()
  private val eventOrganiserRepository: EventOrganiserRepository = mock()
  private val eligibilityRuleRepository: EligibilityRuleRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val bankHolidayService: BankHolidayService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
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
    activitySummaryRepository,
    activityCategoryRepository,
    eventTierRepository,
    eventOrganiserRepository,
    eligibilityRuleRepository,
    activityScheduleRepository,
    prisonPayBandRepository,
    prisonApiClient,
    prisonerSearchApiClient,
    prisonRegimeService,
    bankHolidayService,
    telemetryClient,
    TransactionHandler(),
    outboundEventsService,
    daysInAdvance,
  )

  private val location = Location(
    locationId = 1,
    locationType = "type",
    internalLocationCode = "code",
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
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())
    whenever(eligibilityRuleRepository.findById(eligibilityRuleOver21.eligibilityRuleId)).thenReturn(
      Optional.of(
        eligibilityRuleOver21,
      ),
    )
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))
    whenever(activityRepository.saveAndFlush(any())).thenAnswer {
        invocation ->
      invocation.getArgument(0, ActivityEntity::class.java)
    }

    service().createActivity(createActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    verify(activityCategoryRepository).findById(1)
    verify(eventTierRepository).findByCode("TIER_2")
    verify(eventOrganiserRepository).findByCode("PRISON_STAFF")
    verify(eligibilityRuleRepository).findById(any())

    with(activityCaptor.firstValue) {
      assertThat(eligibilityRules()).hasSize(1)
      assertThat(activityPay()).hasSize(2)
      assertThat(activityMinimumEducationLevel()).hasSize(1)
      assertThat(activityCategory).isEqualTo(activityCategory())
      assertThat(activityTier).isEqualTo(eventTier())
      assertThat(organiser).isEqualTo(eventOrganiser())
    }

    val metricsPropertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to createActivityRequest.prisonCode,
      ACTIVITY_NAME_PROPERTY_KEY to createActivityRequest.summary,
      EVENT_TIER_PROPERTY_KEY to activityCaptor.firstValue.activityTier!!.description,
      EVENT_ORGANISER_PROPERTY_KEY to activityCaptor.firstValue.organiser!!.description,
    )
    verify(telemetryClient).trackEvent(TelemetryEvent.ACTIVITY_CREATED.value, metricsPropertiesMap, activityMetricsMap())
    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, 0)
  }

  @Test
  fun `createActivity - success - creates activity with multi-week schedule`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-9.json")
      .copy(startDate = TimeSource.tomorrow())

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(eventTier())
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))
    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity())

    service(daysInAdvance = 14).createActivity(createActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    verify(activityCategoryRepository).findById(1)
    verify(eventTierRepository).findByCode("TIER_1")

    with(activityCaptor.firstValue) {
      assertThat(activityPay()).hasSize(2)
      assertThat(activityMinimumEducationLevel()).hasSize(1)
      assertThat(activityCategory).isEqualTo(activityCategory())
      assertThat(activityTier).isEqualTo(eventTier())
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
  fun `createActivity - fails when unpaid activity has pay rates`() {
    val createActivityRequestWithPayRates = mapper.read<ActivityCreateRequest>("activity/activity-create-request-1.json")
      .copy(startDate = TimeSource.tomorrow(), paid = false)

    assertThatThrownBy {
      service().createActivity(createActivityRequestWithPayRates, "SCH_ACTIVITY")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Unpaid activity cannot have pay rates associated with it")
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
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode(any())).thenReturn(eventOrganiser())
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
  fun `createActivity - tier code not found`() {
    val activityCreateRequest = activityCreateRequest()

    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode(activityCreateRequest.tierCode!!)).thenReturn(null)

    assertThatThrownBy { service().createActivity(activityCreateRequest(), "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Event tier \"${activityCreateRequest.tierCode}\" not found")
  }

  @Test
  fun `createActivity - organiser code not found`() {
    val activityCreateRequest = activityCreateRequest()

    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventTierRepository.findByCode(activityCreateRequest.organiserCode!!)).thenReturn(null)

    assertThatThrownBy { service().createActivity(activityCreateRequest(), "SCH_ACTIVITY") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Event organiser \"${activityCreateRequest.organiserCode}\" not found")
  }

  @Test
  fun `createActivity - eligibility rule not found`() {
    val activityCreateRequest = activityCreateRequest()

    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())
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
  fun `createActivity - category not in work and foundation is success`() {
    val createActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-11.json")
      .copy(startDate = TimeSource.tomorrow())

    whenever(activityCategoryRepository.findById(8)).thenReturn(Optional.of(notInWorkCategory))
    whenever(eventTierRepository.findByCode("FOUNDATION")).thenReturn(foundationTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())
    whenever(eligibilityRuleRepository.findById(eligibilityRuleOver21.eligibilityRuleId)).thenReturn(
      Optional.of(
        eligibilityRuleOver21,
      ),
    )
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))
    whenever(activityRepository.saveAndFlush(any())).thenAnswer {
        invocation ->
      invocation.getArgument(0, ActivityEntity::class.java)
    }

    service().createActivity(createActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())
    verify(activityCategoryRepository).findById(8)
    verify(eventTierRepository).findByCode("FOUNDATION")
    verify(eligibilityRuleRepository).findById(any())

    with(activityCaptor.firstValue) {
      assertThat(eligibilityRules()).hasSize(1)
      assertThat(activityPay()).hasSize(2)
      assertThat(activityMinimumEducationLevel()).hasSize(1)
      assertThat(activityCategory).isEqualTo(notInWorkCategory)
      assertThat(activityTier).isEqualTo(foundationTier())
    }

    val metricsPropertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to createActivityRequest.prisonCode,
      ACTIVITY_NAME_PROPERTY_KEY to createActivityRequest.summary,
      EVENT_TIER_PROPERTY_KEY to activityCaptor.firstValue.activityTier!!.description,
    )
    verify(telemetryClient).trackEvent(TelemetryEvent.ACTIVITY_CREATED.value, metricsPropertiesMap, activityMetricsMap())
    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, 0)
  }

  @Test
  fun `createActivity - category not in work and tier 1 is not allowed`() {
    val activityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-1.json")
      .copy(startDate = TimeSource.tomorrow(), categoryId = 8)

    whenever(activityCategoryRepository.findById(8)).thenReturn(Optional.of(notInWorkCategory))
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())
    whenever(eligibilityRuleRepository.findById(eligibilityRuleOver21.eligibilityRuleId)).thenReturn(
      Optional.of(
        eligibilityRuleOver21,
      ),
    )
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))
    whenever(activityRepository.saveAndFlush(any())).thenAnswer {
        invocation ->
      invocation.getArgument(0, ActivityEntity::class.java)
    }

    assertThatThrownBy {
      service().createActivity(activityCreateRequest, "SCH_ACTIVITY")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity category NOT IN WORK must be a Foundation Tier")
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
    whenever(activitySummaryRepository.findAllByPrisonCode("MDI"))
      .thenReturn(
        listOf(
          activitySummary(),
          activitySummary(activityName = "English", activityState = ActivityState.ARCHIVED),
        ),
      )

    assertThat(
      service().getActivitiesInPrison(
        "MDI",
        true,
      ),
    ).isEqualTo(listOf(activitySummary()).toModel())

    verify(activitySummaryRepository, times(1)).findAllByPrisonCode("MDI")
  }

  @Test
  fun `getActivitiesInPrison returns all activities including archived activities`() {
    whenever(activitySummaryRepository.findAllByPrisonCode("MDI"))
      .thenReturn(
        listOf(
          activitySummary(),
          activitySummary(activityName = "English", activityState = ActivityState.ARCHIVED),
        ),
      )

    assertThat(
      service().getActivitiesInPrison(
        "MDI",
        false,
      ),
    ).isEqualTo(
      listOf(
        activitySummary(),
        activitySummary(activityName = "English", activityState = ActivityState.ARCHIVED),
      ).toModel(),
    )

    verify(activitySummaryRepository, times(1)).findAllByPrisonCode("MDI")
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
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode(any())).thenReturn(eventOrganiser())
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
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode(any())).thenReturn(eventOrganiser())
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
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode(any())).thenReturn(eventOrganiser())
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
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode(any())).thenReturn(eventOrganiser())
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

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(
      eventTier(
        eventTierId = 1,
        code = "TIER_1",
        description = "Tier 1",
      ),
    )
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))

    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity())

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
  fun `createActivity - Create off-wing activity`() {
    val createdBy = "SCH_ACTIVITY"

    val createInCellActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-6.json")
      .copy(startDate = TimeSource.tomorrow(), inCell = false, offWing = true)

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(eventTier())
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))

    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity())

    service().createActivity(createInCellActivityRequest, createdBy)

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(offWing).isTrue

      with(schedules()[0]) {
        assertThat(internalLocationId).isNull()
        assertThat(internalLocationCode).isNull()
        assertThat(internalLocationDescription).isNull()
      }
    }
  }

  @Test
  fun `createActivity - Cannot be off-wing and in-cell`() {
    val createdBy = "SCH_ACTIVITY"

    val createInCellActivityRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-6.json")
      .copy(startDate = TimeSource.tomorrow(), inCell = true, offWing = true)

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(eventTierRepository.findById(1)).thenReturn(Optional.of(eventTier()))
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))

    assertThatThrownBy {
      service().createActivity(createInCellActivityRequest, createdBy)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity location can only be maximum one of offWing, onWing, inCell, or a specified location")
  }

  @Test
  fun `updateActivity - success`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())

    val savedActivityEntity = activityEntity()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(savedActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(prisonApiClient.getStudyArea("ENGLA")).thenReturn(Mono.just(studyArea))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityCategoryRepository).findById(1)
    verify(eventTierRepository).findByCode("TIER_2")
    verify(eventOrganiserRepository).findByCode("PRISON_STAFF")
    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      with(activityCategory) {
        assertThat(activityCategoryId).isEqualTo(1)
        assertThat(code).isEqualTo("category code")
        assertThat(description).isEqualTo("category description")
      }
      with(activityTier!!) {
        assertThat(eventTierId).isEqualTo(2)
        assertThat(code).isEqualTo("TIER_2")
        assertThat(description).isEqualTo("Tier 2")
      }
      with(organiser!!) {
        assertThat(eventOrganiserId).isEqualTo(1)
        assertThat(code).isEqualTo("PRISON_STAFF")
        assertThat(description).isEqualTo("Prison staff")
      }
    }

    val metricsPropertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to activityCaptor.firstValue.prisonCode,
      ACTIVITY_NAME_PROPERTY_KEY to activityCaptor.firstValue.summary,
      EVENT_TIER_PROPERTY_KEY to "Tier 2",
      EVENT_ORGANISER_PROPERTY_KEY to "Prison staff",
    )
    verify(telemetryClient).trackEvent(TelemetryEvent.ACTIVITY_EDITED.value, metricsPropertiesMap, activityMetricsMap())
    verify(outboundEventsService).send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, savedActivityEntity.schedules().first().activityScheduleId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `updateActivity - duplicate summary`() {
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(activityCategoryRepository.findById(any())).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(prisonPayBandRepository.findByPrisonCode(any())).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)
    whenever(activityRepository.existsActivityByPrisonCodeAndSummary(any(), any())).thenReturn(true)

    val updateDuplicateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-5.json")

    assertThatThrownBy {
      service().updateActivity(
        MOORLAND_PRISON_CODE,
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
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service().updateActivity(MOORLAND_PRISON_CODE, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity Category 1 not found")
  }

  @Test
  fun `updateActivity - tier code not found`() {
    val updatedBy = "SCH_ACTIVITY"
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(null)
    whenever(eventOrganiserRepository.findByCode(any())).thenReturn(eventOrganiser())

    assertThatThrownBy { service().updateActivity(MOORLAND_PRISON_CODE, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Event tier \"TIER_2\" not found")
  }

  @Test
  fun `updateActivity - organiser code not found`() {
    val updatedBy = "SCH_ACTIVITY"
    val savedActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")
    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(eventTierRepository.findByCode(any())).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(null)

    assertThatThrownBy { service().updateActivity(MOORLAND_PRISON_CODE, 1, updateActivityRequest, updatedBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Event organiser \"PRISON_STAFF\" not found")
  }

  @Test
  fun `updateActivity - update category`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-2.json")

    val beforeActivityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(beforeActivityCategory))

    val afterActivityCategory = activityCategory2()
    whenever(activityCategoryRepository.findById(2)).thenReturn(Optional.of(afterActivityCategory))
    whenever(eventTierRepository.findById(1)).thenReturn(Optional.of(eventTier()))
    whenever(eventOrganiserRepository.findById(1)).thenReturn(Optional.of(eventOrganiser()))

    val beforeActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-1.json")

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(beforeActivityEntity)

    val afterActivityEntity: ActivityEntity = mapper.read("activity/updated-activity-entity-1.json")

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, updateActivityRequest, "SCH_ACTIVITY")

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
    val newEndDate = TimeSource.today().plusYears(1)
    val beforeActivityEntity: ActivityEntity = mapper.read("activity/activity-entity-3.json")
    val updateActivityRequest = ActivityUpdateRequest(endDate = newEndDate)

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
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(beforeActivityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)

    service().updateActivity(MOORLAND_PRISON_CODE, 1, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      endDate isEqualTo newEndDate
      schedules().first().endDate isEqualTo newEndDate
      schedules().first().allocations().single { it.prisonerNumber == "123456" }.endDate isEqualTo newEndDate
      schedules().first().allocations().single { it.prisonerNumber == "654321" }.endDate isEqualTo null
    }
  }

  @Test
  fun `updateActivity - update pay`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-3.json")
    val activityEntity: ActivityEntity = activityEntity(noPayBands = true).copy(activityId = 17)

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        17,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())

    service().updateActivity(MOORLAND_PRISON_CODE, 17, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(activityPay()).hasSize(1)
    }
  }

  @Test
  fun `updateActivity - update pay band where someone is allocated to it`() {
    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-6.json")
    val activityEntity: ActivityEntity = activityEntity()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        17,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activityEntity)
    whenever(activityRepository.saveAndFlush(any())).thenReturn(activityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())

    val prisonerNumbers = activityEntity.schedules().first().allocations().map { it.prisonerNumber }
    val prisoners = prisonerNumbers.map { PrisonerSearchPrisonerFixture.instance(prisonerNumber = it) }
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers)).thenReturn(prisoners)

    service().updateActivity(MOORLAND_PRISON_CODE, 17, updateActivityRequest, "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(activityPay()).hasSize(1)
      assertThat(schedules().first().allocations().first().payBand?.prisonPayBandId).isEqualTo(updateActivityRequest.pay!!.first().payBandId)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 0L)
  }

  @Test
  fun `updateActivity - update start date fails if new date not in future`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(startDate = TimeSource.today()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Start date must be in the future.")
  }

  @Test
  fun `updateActivity - update start date fails if new date after end date`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(
        MOORLAND_PRISON_CODE,
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
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(startDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Activity already started.")
  }

  @Test
  fun `updateActivity - update start date fails if activity has allocations already started`() {
    val activity = activityEntity(startDate = TimeSource.today(), endDate = TimeSource.tomorrow().plusWeeks(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(startDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity start date cannot be changed. Activity already started.")
  }

  @Test
  fun `updateActivity - update end date fails if removeEndDate is also true`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(
        MOORLAND_PRISON_CODE,
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
          daysOfWeek = DayOfWeek.entries.toSet(),
        ),
      )
    }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThat(activity.endDate).isEqualTo(TimeSource.tomorrow().plusDays(1))
    activity.schedules().forEach { activitySchedule ->
      assertThat(
        activitySchedule.instances().find { i -> i.sessionDate > TimeSource.tomorrow().plusDays(1) },
      ).isNull()
    }

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(removeEndDate = true), "TEST")

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
      val everydaySlot = it.addSlot(1, LocalTime.NOON to LocalTime.NOON.plusHours(1), DayOfWeek.entries.toSet())
      it.addInstance(TimeSource.tomorrow().plusDays(1), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(2), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(3), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(4), everydaySlot)
    }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(endDate = TimeSource.tomorrow()), "TEST")

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
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    val newEndDate = activity.endDate!!.plusDays(4)

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(endDate = newEndDate), "TEST")

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
      val everydaySlot = it.addSlot(1, LocalTime.NOON to LocalTime.NOON.plusHours(1), DayOfWeek.entries.toSet())
      it.addInstance(TimeSource.tomorrow().plusDays(1), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(2), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(3), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(4), everydaySlot)
      it.addInstance(TimeSource.tomorrow().plusDays(5), everydaySlot)
    }

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(
      MOORLAND_PRISON_CODE,
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
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(endDate = TimeSource.tomorrow()), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity cannot be updated because it is now archived.")
  }

  @Test
  fun `updateActivity - fails if activity not found`() {
    whenever(activityRepository.findByActivityIdAndPrisonCode(1, MOORLAND_PRISON_CODE)).thenReturn(null)

    assertThatThrownBy {
      service().updateActivity(PENTONVILLE_PRISON_CODE, 1, ActivityUpdateRequest(endDate = TimeSource.tomorrow()), "TEST")
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
      prisonRegime().amStart to prisonRegime().amFinish,
      setOf(tomorrow.dayOfWeek),
    )
    val slot2 = schedule.addSlot(
      1,
      prisonRegime().pmStart to prisonRegime().pmFinish,
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
      prisonRegime().amStart to prisonRegime().amFinish,
      setOf(tomorrow.dayOfWeek),
    )
    val slot2 = schedule.addSlot(
      1,
      prisonRegime().pmStart to prisonRegime().pmFinish,
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
            LocalTime.NOON to LocalTime.NOON.plusHours(1),
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
            LocalTime.NOON to LocalTime.NOON.plusHours(1),
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
        slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
        daysOfWeek = setOf(tomorrow.dayOfWeek, tomorrow.plusDays(2).dayOfWeek),
      )
      addSlot(
        weekNumber = 2,
        slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
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
        slotTimes = LocalTime.of(9, 0) to LocalTime.of(12, 0),
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
  fun `updateActivity - update slots where there is an exclusion`() {
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

    activity.addSchedule(
      activitySchedule(
        activity,
        scheduleWeeks = 1,
        noSlots = true,
        noInstances = true,
        noAllocations = true,
      ),
    ).apply {
      val slot = addSlot(
        weekNumber = 1,
        slotTimes = LocalTime.of(9, 0) to LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
      allocatePrisoner(
        prisonerNumber = "A1111BB".toPrisonerNumber(),
        bookingId = 20002,
        payBand = lowPayBand,
        allocatedBy = "Mr Blogs",
        startDate = startDate,
      ).apply {
        this.updateExclusion(slot, setOf(tomorrow.dayOfWeek))
      }
    }

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "AM",
            monday = tomorrow.dayOfWeek != DayOfWeek.MONDAY,
            tuesday = tomorrow.dayOfWeek != DayOfWeek.TUESDAY,
            wednesday = tomorrow.dayOfWeek != DayOfWeek.WEDNESDAY,
            thursday = tomorrow.dayOfWeek != DayOfWeek.THURSDAY,
            friday = tomorrow.dayOfWeek != DayOfWeek.FRIDAY,
            saturday = tomorrow.dayOfWeek != DayOfWeek.SATURDAY,
            sunday = tomorrow.dayOfWeek != DayOfWeek.SUNDAY,
          ),
        ),
      ),
      updatedBy = "TEST",
    )

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 0L)
  }

  @Test
  fun `updateActivity - update slots where there is an exclusion - event not emitted if exclusion is not affected`() {
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

    activity.addSchedule(
      activitySchedule(
        activity,
        scheduleWeeks = 1,
        noSlots = true,
        noInstances = true,
        noAllocations = true,
      ),
    ).apply {
      val slot = addSlot(
        weekNumber = 1,
        slotTimes = LocalTime.of(9, 0) to LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
      allocatePrisoner(
        prisonerNumber = "A1111BB".toPrisonerNumber(),
        bookingId = 20002,
        payBand = lowPayBand,
        allocatedBy = "Mr Blogs",
        startDate = startDate,
      ).apply {
        this.updateExclusion(slot, setOf(tomorrow.dayOfWeek))
      }
    }

    service().updateActivity(
      activity.prisonCode,
      activity.activityId,
      ActivityUpdateRequest(
        slots = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = "AM",
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = true,
            sunday = true,
          ),
        ),
      ),
      updatedBy = "TEST",
    )

    verify(outboundEventsService, never()).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 0L)
  }

  @Test
  fun `updateActivity - update to off-wing`() {
    val activity = activityEntity()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(offWing = true), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(offWing).isTrue
    }
  }

  @Test
  fun `updateActivity - cannot be both in-cell and off-wing`() {
    val activity = activityEntity()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(offWing = true, inCell = true), "TEST")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity location can only be maximum one of offWing, onWing, inCell, or a specified location")
  }

  @Test
  fun `updateActivity - add organiser`() {
    val activity = activityEntity(
      organiser = eventOrganiser(
        eventOrganiserId = 1,
        code = "PRISON_STAFF",
        description = "Prison staff",
      ),
    )

    whenever(eventOrganiserRepository.findByCode("PRISONER")).thenReturn(
      eventOrganiser(
        eventOrganiserId = 2,
        code = "PRISONER",
        description = "A prisoner or group of prisoners",
      ),
    )
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier())

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    activity.organiser isEqualTo eventOrganiser(
      eventOrganiserId = 1,
      code = "PRISON_STAFF",
      description = "Prison staff",
    )

    service().updateActivity(
      MOORLAND_PRISON_CODE,
      1,
      ActivityUpdateRequest(tierCode = "TIER_2", organiserCode = "PRISONER"),
      "TEST",
    )

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      this.organiser!! isEqualTo eventOrganiser(
        eventOrganiserId = 2,
        code = "PRISONER",
        description = "A prisoner or group of prisoners",
      )
    }

    val metricsPropertiesMap = mapOf(
      PRISON_CODE_PROPERTY_KEY to activityCaptor.firstValue.prisonCode,
      ACTIVITY_NAME_PROPERTY_KEY to activityCaptor.firstValue.summary,
      EVENT_TIER_PROPERTY_KEY to "Tier 2",
      EVENT_ORGANISER_PROPERTY_KEY to "A prisoner or group of prisoners",
    )
    verify(telemetryClient).trackEvent(TelemetryEvent.ACTIVITY_EDITED.value, metricsPropertiesMap, activityMetricsMap())
  }

  @Test
  fun `updateActivity - add organiser fails if activity not tier 2`() {
    val activity = activityEntity()

    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())
    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(
      eventTier(
        eventTierId = 1,
        code = "TIER_1",
        description = "Tier 1",
      ),
    )

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(
        MOORLAND_PRISON_CODE,
        1,
        ActivityUpdateRequest(tierCode = "TIER_1", organiserCode = "PRISON_STAFF"),
        "TEST",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add activity organiser unless activity is Tier 2.")

    verify(activityRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `updateActivity - remove organiser if not tier 2`() {
    val activity = activityEntity()

    assertThat(activity.organiser).isNotNull

    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(
      eventTier(
        eventTierId = 1,
        code = "TIER_1",
        description = "Tier 1",
      ),
    )

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(tierCode = "TIER_1"), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(this.organiser).isNull()
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
        slotTimes = LocalTime.of(9, 0) to LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
      addSlot(
        weekNumber = 2,
        slotTimes = LocalTime.of(9, 0) to LocalTime.of(12, 0),
        daysOfWeek = setOf(tomorrow.dayOfWeek),
      )
    }

    assertThat(schedule.instances()).isEmpty()

    service(7).addScheduleInstances(schedule)

    schedule.instances() hasSize 1

    service(14).addScheduleInstances(schedule)

    schedule.instances() hasSize 2
  }

  @Test
  fun `updateActivity - paid to unpaid when no allocations`() {
    val activity = activitySchedule(activityEntity(paid = true, noSchedules = true), noAllocations = true).activity

    activity.paid isBool true

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(paid = false), "SCH_ACTIVITY")

    activity.paid isBool false
    activity.activityPay() hasSize 0
  }

  @Test
  fun `updateActivity - unpaid to paid when no allocations`() {
    val activity = activitySchedule(activityEntity(paid = false, noSchedules = true, noPayBands = true), noAllocations = true).activity

    activity.paid isBool false

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(prisonPayBandsLowMediumHigh())

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(paid = true, pay = listOf(ActivityPayCreateRequest(incentiveNomisCode = "123", incentiveLevel = "level", payBandId = 1))), "SCH_ACTIVITY")

    activity.paid isBool true
  }

  @Test
  fun `updateActivity - must include pay rates when changing from unpaid to paid`() {
    val activity = activitySchedule(activityEntity(paid = false, noSchedules = true, noPayBands = true), noAllocations = true).activity

    activity.paid isBool false

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(activity)

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(paid = true), "SCH_ACTIVITY")
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Activity '1' must have at least one pay rate.")
  }

  @Test
  fun `updateActivity - attendance required from NO to YES for tier 1 is successful`() {
    val savedActivityEntity: ActivityEntity = activityEntity()
    savedActivityEntity.attendanceRequired = false
    val eventTier = eventTier(eventTierId = 1, code = "TIER_1", description = "Tier 1")
    savedActivityEntity.activityTier = eventTier

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val afterActivityEntity: ActivityEntity = activityEntity()
    afterActivityEntity.attendanceRequired = true
    afterActivityEntity.activityTier = eventTier

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(attendanceRequired = true), "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(attendanceRequired).isTrue()
    }
  }

  @Test
  fun `updateActivity - attendance required from NO to YES for tier 2 is successful`() {
    val savedActivityEntity: ActivityEntity = activityEntity()
    savedActivityEntity.attendanceRequired = false

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val afterActivityEntity: ActivityEntity = activityEntity()
    afterActivityEntity.attendanceRequired = true
    afterActivityEntity.activityTier = eventTier()

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(attendanceRequired = true), "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(attendanceRequired).isTrue()
    }
  }

  @Test
  fun `updateActivity - attendance required from YES to NO for tier 1 is unsuccessful`() {
    val eventTier = eventTier(eventTierId = 1, code = "TIER_1", description = "Tier 1")
    val savedActivityEntity: ActivityEntity = activityEntity()
    savedActivityEntity.attendanceRequired = true
    savedActivityEntity.activityTier = eventTier

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(eventTier)
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(attendanceRequired = false), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance cannot be from YES to NO for a 'TIER_1' activity.")
  }

  @Test
  fun `updateActivity - attendance required from YES to NO for tier 2 is unsuccessful`() {
    val savedActivityEntity: ActivityEntity = activityEntity()
    savedActivityEntity.attendanceRequired = true

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    whenever(activityCategoryRepository.findById(2)).thenReturn(Optional.of(activityCategory()))
    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier())
    whenever(eventOrganiserRepository.findByCode("PRISON_STAFF")).thenReturn(eventOrganiser())

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(attendanceRequired = false), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance cannot be from YES to NO for a 'TIER_2' activity.")
  }

  @Test
  fun `updateActivity - attendance required from YES to NO for unpaid foundation tier is successful`() {
    val eventTier = eventTier(eventTierId = 3, code = "FOUNDATION", description = "Foundation")
    val savedActivityEntity: ActivityEntity = activityEntity(paid = false, noSchedules = true, noPayBands = true)
    savedActivityEntity.attendanceRequired = true
    savedActivityEntity.activityTier = eventTier

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val afterActivityEntity: ActivityEntity = activityEntity()
    afterActivityEntity.attendanceRequired = false
    afterActivityEntity.activityTier = eventTier

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(attendanceRequired = false), "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(attendanceRequired).isFalse()
    }
  }

  @Test
  fun `updateActivity - unpaid to paid where existing entity set to attendance required is successful`() {
    val eventTier = eventTier(eventTierId = 3, code = "FOUNDATION", description = "Foundation")
    val savedActivityEntity: ActivityEntity = activityEntity(paid = false, noSchedules = true, noPayBands = true)
    savedActivityEntity.attendanceRequired = true
    savedActivityEntity.activityTier = eventTier

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val afterActivityEntity: ActivityEntity = activityEntity()
    afterActivityEntity.attendanceRequired = false
    afterActivityEntity.activityTier = eventTier

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(attendanceRequired = false), "SCH_ACTIVITY")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(attendanceRequired).isFalse()
    }
  }

  @Test
  fun `updateActivity - unpaid to paid where existing entity not set to attendance required is unsuccessful`() {
    val savedActivityEntity: ActivityEntity = activityEntity(paid = false, noSchedules = true, noPayBands = true)
    savedActivityEntity.attendanceRequired = false

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    var activityUpdateRequest = ActivityUpdateRequest(paid = true, pay = listOf(ActivityPayCreateRequest(incentiveNomisCode = "123", incentiveLevel = "level", payBandId = 1)))

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, activityUpdateRequest, "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity '1' cannot be paid as attendance is not required.")
  }

  @Test
  fun `updateActivity - unpaid to paid and attendance required where existing entity not set to attendance required is successful`() {
    val savedActivityEntity: ActivityEntity = activityEntity(paid = false, noSchedules = true, noPayBands = true)
    savedActivityEntity.attendanceRequired = false

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val afterActivityEntity: ActivityEntity = activityEntity()
    afterActivityEntity.attendanceRequired = false

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(paid = true, pay = listOf(ActivityPayCreateRequest(incentiveNomisCode = "123", incentiveLevel = "level", payBandId = 1)), attendanceRequired = true), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(attendanceRequired).isTrue()
      assertThat(paid).isTrue()
    }
  }

  @Test
  fun `updateActivity - category not in work and foundation tier is success`() {
    val savedActivityEntity: ActivityEntity = activityEntity()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    val afterActivityEntity: ActivityEntity = activityEntity()

    whenever(activityRepository.saveAndFlush(any())).thenReturn(afterActivityEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBandsLowMediumHigh())
    whenever(prisonApiClient.getEducationLevel("1")).thenReturn(Mono.just(educationLevel))
    whenever(activityCategoryRepository.findById(8)).thenReturn(Optional.of(notInWorkCategory))
    whenever(eventTierRepository.findByCode("FOUNDATION")).thenReturn(foundationTier())

    service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(tierCode = "FOUNDATION", categoryId = 8), "TEST")

    verify(activityRepository).saveAndFlush(activityCaptor.capture())

    with(activityCaptor.firstValue) {
      assertThat(attendanceRequired).isTrue()
      assertThat(paid).isTrue()
    }
  }

  @Test
  fun `updateActivity - change tier to tier 1 with category not in work is not allowed`() {
    val savedActivityEntity: ActivityEntity = activityEntity(category = notInWorkCategory)
    savedActivityEntity.organiser = null
    savedActivityEntity.activityTier = foundationTier()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    whenever(eventTierRepository.findByCode("TIER_1")).thenReturn(eventTier(eventTierId = 1, code = "TIER_1", description = "Tier 1"))

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(tierCode = "TIER_1"), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity category SAA_NOT_IN_WORK can only be for a Foundation Tier.")
  }

  @Test
  fun `updateActivity - change tier to tier 2 with category not in work is not allowed`() {
    val savedActivityEntity: ActivityEntity = activityEntity(category = notInWorkCategory)
    savedActivityEntity.organiser = null
    savedActivityEntity.activityTier = foundationTier()

    whenever(
      activityRepository.findByActivityIdAndPrisonCodeWithFilters(
        1,
        MOORLAND_PRISON_CODE,
        LocalDate.now(),
      ),
    ).thenReturn(savedActivityEntity)

    whenever(eventTierRepository.findByCode("TIER_2")).thenReturn(eventTier(eventTierId = 2, code = "TIER_2", description = "Tier 2"))

    assertThatThrownBy {
      service().updateActivity(MOORLAND_PRISON_CODE, 1, ActivityUpdateRequest(tierCode = "TIER_2"), "TEST")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity category SAA_NOT_IN_WORK can only be for a Foundation Tier.")
  }
}
