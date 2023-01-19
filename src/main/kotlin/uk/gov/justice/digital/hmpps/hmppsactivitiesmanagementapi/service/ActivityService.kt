package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
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
      activityRepository.findOrThrowNotFound(activityId)
    )

  fun getActivitiesByCategoryInPrison(
    prisonCode: String,
    categoryId: Long
  ) =
    activityCategoryRepository.findOrThrowNotFound(categoryId).let {
      activityRepository.getAllByPrisonCodeAndActivityCategory(prisonCode, it).toModelLite()
    }

  fun getActivitiesInPrison(
    prisonCode: String
  ) = activityRepository.getAllByPrisonCode(prisonCode).toModelLite()

  fun getSchedulesForActivity(activityId: Long) =
    activityRepository.findOrThrowNotFound(activityId)
      .let { activityScheduleRepository.getAllByActivity(it).toModelLite() }

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun createActivity(request: ActivityCreateRequest, createdBy: String): ModelActivity {

    val category = activityCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)
    val tier = request.tierId?.let { activityTierRepository.findOrThrowIllegalArgument(it) }
    val activity = transform(request, category, tier, createdBy)
    val eligibilityRules = request.eligibilityRuleIds.map {
      ActivityEligibility(eligibilityRule = eligibilityRuleRepository.findOrThrowIllegalArgument(it), activity = activity)
    }

    eligibilityRules.forEach { aee -> activity.eligibilityRules.add(aee) }

    transform(request.pay, activity).forEach { activity.activityPay.add(it) }

    try {
      return transform(activityRepository.saveAndFlush(activity))
    } catch (ex: DataIntegrityViolationException) {
      throw IllegalArgumentException("Duplicate activity name detected for this prison (${activity.prisonCode}): '${activity.summary}'")
    }
  }
}
