package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

@Service
class ActivityService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val activityTierRepository: ActivityTierRepository,
  private val eligibilityRuleRepository: EligibilityRuleRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
) {
  fun getActivityById(activityId: Long) =
    transform(
      activityRepository.findById(activityId).orElseThrow {
        EntityNotFoundException(
          "$activityId"
        )
      }
    )

  fun getActivitiesByCategoryInPrison(
    prisonCode: String,
    categoryId: Long
  ): List<ActivityLite> {
    val activityCategory = activityCategoryRepository.findById(categoryId)
      .orElseThrow { EntityNotFoundException("Activity category $categoryId not found") }

    return activityRepository.getAllByPrisonCodeAndActivityCategory(prisonCode, activityCategory)
      .toModelLite()
  }

  fun getSchedulesForActivity(activityId: Long): List<ActivityScheduleLite> {
    val activity = activityRepository.findById(activityId)
      .orElseThrow { EntityNotFoundException("Activity $activityId not found") }

    return activityScheduleRepository.getAllByActivity(activity).toModelLite()
  }

  fun createActivity(activityCreateRequest: ActivityCreateRequest, createdBy: String): ModelActivity {

    val categoryEntity = activityCategoryRepository.findById(activityCreateRequest.categoryId!!)
      .orElseThrow { IllegalArgumentException("Activity category ${activityCreateRequest.categoryId} not found") }

    val tierEntity: ActivityTier? = activityCreateRequest.tierId?.let {
      activityTierRepository.findById(it)
        .orElseThrow { IllegalArgumentException("Activity tier ${activityCreateRequest.tierId} not found") }
    }

    val activityEntity = transform(activityCreateRequest, categoryEntity, tierEntity, createdBy)
    val activityEligibilityEntityList = activityCreateRequest.eligibilityRuleIds.map {
      ActivityEligibility(
        activityEligibilityId = null,
        eligibilityRule = eligibilityRuleRepository.findById(it)
          .orElseThrow { IllegalArgumentException("Eligibility rule $it not found") },
        activity = activityEntity
      )
    }
    activityEligibilityEntityList.forEach { aee -> activityEntity.eligibilityRules.add(aee) }
    val activityPayList = transform(activityCreateRequest.pay, activityEntity)
    activityPayList.forEach { aee -> activityEntity.activityPay.add(aee) }
    try {
      return transform(activityRepository.save(activityEntity))
    } catch (ex: DataIntegrityViolationException) {
      throw throw IllegalArgumentException("Duplicate activity summary detected for this prison (${activityEntity.prisonCode}): '${activityEntity.summary}'")
    }
  }
}
