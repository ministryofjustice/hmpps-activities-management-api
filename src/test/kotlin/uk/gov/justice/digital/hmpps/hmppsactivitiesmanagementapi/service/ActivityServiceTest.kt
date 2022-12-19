package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityEligibilityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityPayRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import java.util.Optional
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as ActivityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility as ActivityEligibilityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay as ActivityPayEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EligibilityRule as EligibilityRuleEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

class ActivityServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityTierRepository: ActivityTierRepository = mock()
  private val activityPayRepository: ActivityPayRepository = mock()
  private val activityEligibilityRepository: ActivityEligibilityRepository = mock()
  private val eligibilityRuleRepository: EligibilityRuleRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()

  val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  private val service = ActivityService(
    activityRepository,
    activityCategoryRepository,
    activityTierRepository,
    activityPayRepository,
    activityEligibilityRepository,
    eligibilityRuleRepository,
    activityScheduleRepository
  )

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
    val activityEligibility = ActivityEligibilityEntity(activityEligibilityId = 1, eligibilityRule = eligibilityRule, activity = savedActivityEntity)
    val savedActivityEligibility: MutableList<ActivityEligibilityEntity> = mutableListOf(activityEligibility)

    whenever(activityRepository.save(any())).thenReturn(savedActivityEntity)
    whenever(eligibilityRuleRepository.findById(1L)).thenReturn(Optional.of(eligibilityRule))
    whenever(activityEligibilityRepository.saveAll(any<MutableList<ActivityEligibilityEntity>>())).thenReturn(savedActivityEligibility)

    val activityPay1 = ActivityPayEntity(activityPayId = 73, incentiveLevel = "Basic", payBand = "A", rate = 125, pieceRate = 150, pieceRateItems = 10, activity = savedActivityEntity)
    val activityPay2 = ActivityPayEntity(activityPayId = 74, incentiveLevel = "Standard", payBand = "B", rate = 160, pieceRate = 180, pieceRateItems = 15, activity = savedActivityEntity)
    whenever(activityPayRepository.saveAll(any<MutableList<ActivityPayEntity>>())).thenReturn(mutableListOf(activityPay1, activityPay2))

    val result = service.createActivity(createActivityRequest, createdBy)

    with(result) {
      assertThat(eligibilityRules).hasSize(1)
      with(category) {
        assertThat(id).isEqualTo(1)
        assertThat(code).isEqualTo("EDU")
        assertThat(description).isEqualTo("Education")
      }
      with(tier) {
        assertThat(id).isEqualTo(1)
        assertThat(code).isEqualTo("Tier1")
        assertThat(description).isEqualTo("Work, education and maintenance")
      }
    }

    verify(activityCategoryRepository).findById(1)
    verify(activityTierRepository).findById(1)
    verify(activityRepository).save(any())
    verify(eligibilityRuleRepository).findById(any())
    verify(activityPayRepository).saveAll(any<MutableList<ActivityPayEntity>>())
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
      .hasMessage("Activity category 1 not found")
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
