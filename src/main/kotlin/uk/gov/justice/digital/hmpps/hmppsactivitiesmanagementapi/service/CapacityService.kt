package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class CapacityService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
) {
  fun getActivityCategoryCapacityAndAllocated(
    prisonCode: String,
    categoryId: Long,
  ): CapacityAndAllocated {
    val activityCategory = activityCategoryRepository.findOrThrowNotFound(categoryId)
    val activities = activityRepository.getAllByPrisonCodeAndActivityCategory(prisonCode, activityCategory)
    val capacity = activities.sumOf { a -> sumOfScheduleCapacities(a) }
    val allocated = activities.sumOf { a -> sumOfScheduleAllocations(a) }

    return CapacityAndAllocated(capacity = capacity, allocated = allocated)
  }

  fun getActivityCapacityAndAllocated(
    activityId: Long,
  ): CapacityAndAllocated {
    val activity = activityRepository.findOrThrowNotFound(activityId)
    val capacity = sumOfScheduleCapacities(activity)
    val allocated = sumOfScheduleAllocations(activity)

    return CapacityAndAllocated(capacity = capacity, allocated = allocated)
  }

  fun getActivityScheduleCapacityAndAllocated(
    scheduleId: Long,
  ): CapacityAndAllocated {
    val schedule = activityScheduleRepository.findOrThrowNotFound(scheduleId)
    val capacity = schedule.capacity
    val allocated = schedule.getAllocationsForToday().size

    return CapacityAndAllocated(capacity = capacity, allocated = allocated)
  }

  private fun sumOfScheduleCapacities(activity: Activity): Int {
    return activity.schedules().sumOf { schedule -> schedule.capacity }
  }

  private fun sumOfScheduleAllocations(activity: Activity): Int {
    return activity.schedules().sumOf { schedule -> schedule.getAllocationsForToday().size }
  }

  private fun ActivitySchedule.getAllocationsForToday(): List<Allocation> {
    return this.allocations().filterNot { it.status(PrisonerStatus.ENDED) }
  }
}
