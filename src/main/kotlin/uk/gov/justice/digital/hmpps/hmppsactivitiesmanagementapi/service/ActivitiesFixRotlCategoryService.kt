package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.ActivityCategoryRepository

@Service
class ActivitiesFixRotlCategoryService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val activityService: ActivityService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val ADMIN_USERNAME = "activities-management-admin-1"
    private const val ROTL_CATEGORY_CODE = "SAA_ROTL"
    const val MAX_ACTIVITIES_TO_FIX = 175
  }

  fun fixCategoriesAndLocations() {
    val rotlCategory = activityCategoryRepository.findByCode(ROTL_CATEGORY_CODE)
      ?: throw IllegalStateException("Activity category with code $ROTL_CATEGORY_CODE not found")

    val activities = activityRepository.findOutsideWorkActivitiesNeedingRotlFix()

    if (activities.size > MAX_ACTIVITIES_TO_FIX) {
      throw IllegalStateException("Aborting: found ${activities.size} activities needing the ROTL fix, which exceeds the safety limit of $MAX_ACTIVITIES_TO_FIX. No changes have been made.")
    }

    log.info("Found ${activities.size} outside work activities needing the ROTL category and location flags fixed")

    activities.forEach { activity ->
      runCatching {
        activityService.updateActivity(
          activity.prisonCode,
          activity.activityId,
          ActivityUpdateRequest(
            categoryId = rotlCategory.activityCategoryId,
            onWing = false,
            offWing = false,
            inCell = false,
          ),
          ADMIN_USERNAME,
          adminMode = true,
        )
      }
        .onSuccess { log.info("Fixed ROTL category and location flags for activity '${activity.activityId}'") }
        .onFailure { log.error("Failed to fix ROTL category and location flags for activity '${activity.activityId}'", it) }
    }

    log.info("Finished fixing ROTL category and location flags for outside work activities")
  }
}
