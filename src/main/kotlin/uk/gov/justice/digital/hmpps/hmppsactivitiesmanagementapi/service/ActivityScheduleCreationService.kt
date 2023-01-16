package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityScheduleCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class ActivityScheduleCreationService(
  private val activityRepository: ActivityRepository,
  private val prisonApiClient: PrisonApiClient
) {

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun createSchedule(
    activityId: Long,
    request: ActivityScheduleCreateRequest,
    allocatedBy: String
  ): ActivityScheduleLite {
    val activity = activityRepository.findOrThrowNotFound(activityId).let { activity ->
      val location = prisonApiClient.getLocation(activity, request)

      activity.addSchedule(
        description = request.description!!,
        internalLocationId = location.locationId.toInt(),
        internalLocationCode = location.internalLocationCode ?: "",
        internalLocationDescription = location.description,
        capacity = request.capacity!!,
        startDate = request.startDate!!,
        endDate = request.endDate
      )

      activityRepository.saveAndFlush(activity)
    }

    return activity.schedules.last().toModelLite()
  }

  private fun PrisonApiClient.getLocation(activity: Activity, request: ActivityScheduleCreateRequest): Location {
    val location = prisonApiClient.getLocation(request.locationId!!).block()!!
    failIfPrisonsDiffer(activity, location)
    return location
  }

  private fun failIfPrisonsDiffer(activity: Activity, location: Location) {
    if (activity.prisonCode != location.agencyId) {
      throw IllegalArgumentException("The activities prison '${activity.prisonCode}' does not match that of the locations '${location.agencyId}'")
    }
  }
}
