package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import javax.persistence.EntityNotFoundException

@Service
class ActivityService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
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
}
