package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import javax.persistence.EntityNotFoundException

@Service
class ActivityService(private val activityRepository: ActivityRepository) {
  fun getActivityById(activityId: Long) = transform(
    activityRepository
      .findById(activityId)
      .orElseThrow {
        EntityNotFoundException(
          "$activityId"
        )
      }
  )
}
