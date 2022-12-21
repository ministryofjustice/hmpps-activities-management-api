package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEventsService
import javax.persistence.PostPersist

@Component
class ActivityEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(activity: Activity) {
    runCatching {
      outboundEventsService.send(OutboundEvent.ACTIVITY_CREATED, activity.activityId!!)
    }.onFailure {
      log.error("Failed to send activity creation event for activity ${activity.activityId}", it)
    }
  }
}
