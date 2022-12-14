package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import java.util.Optional
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as ActivityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule as EligibilityRuleEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

class ActivityServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityTierRepository: ActivityTierRepository = mock()
  private val eligibilityRuleRepository: EligibilityRuleRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()

  val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  @Captor
  private lateinit var activityEntityCaptor: ArgumentCaptor<ActivityEntity>

  private val service = ActivityService(
    activityRepository,
    activityCategoryRepository,
    activityTierRepository,
    eligibilityRuleRepository,
    activityScheduleRepository
  )

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `createActivity - success`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-1.json"),
      object : TypeReference<ActivityCreateRequest>() {}
    )

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))

    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))

    val savedActivityEntity: ActivityEntity = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-entity-1.json"),
      object : TypeReference<ActivityEntity>() {}
    )

    val eligibilityRule = EligibilityRuleEntity(eligibilityRuleId = 1, code = "ER1", "Eligibility rule 1")
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))
    whenever(activityRepository.saveAndFlush(activityEntityCaptor.capture())).thenReturn(savedActivityEntity)

    service.createActivity(createActivityRequest, createdBy)

    val activityArg: ActivityEntity = activityEntityCaptor.value

    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)
    verify(eligibilityRuleRepository).findById(any())
    verify(activityRepository).saveAndFlush(activityArg)

    with(activityArg) {
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(activityPay.size).isEqualTo(2)
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
  fun `createActivity - category id not found`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-1.json"),
      object : TypeReference<ActivityCreateRequest>() {}
    )

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity category 1 not found")
  }

  @Test
  fun `createActivity - tier id not found`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-1.json"),
      object : TypeReference<ActivityCreateRequest>() {}
    )

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity tier 1 not found")
  }

  @Test
  fun `createActivity - eligibility rule not found`() {
    val createdBy = "SCH_ACTIVITY"

    val createActivityRequest: ActivityCreateRequest = mapper.readValue(
      this::class.java.getResource("/__files/activity/activity-create-request-1.json"),
      object : TypeReference<ActivityCreateRequest>() {}
    )

    val activityCategory = activityCategory()
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory))
    val activityTier = activityTier()
    whenever(activityTierRepository.findById(1)).thenReturn(Optional.of(activityTier))
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.empty())

    assertThatThrownBy { service.createActivity(createActivityRequest, createdBy) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Eligibility rule 1 not found")
  }

  @Test
  fun `getActivityById returns an activity for known activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity()))

    assertThat(service.getActivityById(1)).isInstanceOf(ModelActivity::class.java)
  }

  @Test
  fun `getActivityById throws entity not found exception for unknown activity ID`() {
    assertThatThrownBy { service.getActivityById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("-1")
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
        1
      )
    ).isEqualTo(listOf(activityEntity()).toModelLite())

    verify(activityRepository, times(1)).getAllByPrisonCodeAndActivityCategory("MDI", category)
  }

  @Test
  fun `getActivitiesByCategoryInPrison throws entity not found exception for unknown category ID`() {
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getActivitiesByCategoryInPrison("MDI", 1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity category 1 not found")
  }

  @Test
  fun `getActivitiesInPrison returns list of activities`() {
    whenever(activityRepository.getAllByPrisonCode("MDI"))
      .thenReturn(listOf(activityEntity()))

    assertThat(
      service.getActivitiesInPrison(
        "MDI",
      )
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
}
