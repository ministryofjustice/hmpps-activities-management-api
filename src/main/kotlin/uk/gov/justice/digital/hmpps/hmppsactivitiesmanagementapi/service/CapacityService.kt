package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import java.time.LocalDate
import javax.persistence.EntityNotFoundException

@Service
class CapacityService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
) {
  fun getActivityCategoryCapacityAndAllocated(
    prisonCode: String,
    categoryId: Long
  ): CapacityAndAllocated {
    val activityCategory = activityCategoryRepository.findById(categoryId)
      .orElseThrow { EntityNotFoundException("Activity category $categoryId not found") }

    val activities =
      activityRepository.getAllByPrisonCodeAndActivityCategory(prisonCode, activityCategory)

    val capacity = activities.sumOf { a -> sumOfScheduleCapacities(a) }
    val allocated = activities.sumOf { a -> sumOfScheduleAllocations(a) }

    return CapacityAndAllocated(capacity = capacity, allocated = allocated)
  }

  private fun sumOfScheduleCapacities(activity: Activity): Int {
    return activity.schedules.sumOf { schedule -> schedule.capacity }
  }

  private fun sumOfScheduleAllocations(activity: Activity): Int {
    return activity.schedules.sumOf { schedule -> schedule.getAllocationsOnDate(LocalDate.now()).size }
  }
}
