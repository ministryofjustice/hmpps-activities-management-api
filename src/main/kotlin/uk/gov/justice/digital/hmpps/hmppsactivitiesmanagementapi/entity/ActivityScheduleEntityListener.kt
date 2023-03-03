package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEventsService

@Component
class ActivityScheduleEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(schedule: ActivitySchedule) {
    runCatching {
      outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, schedule.activityScheduleId)
    }.onFailure {
      log.error(
        "Failed to send activity schedule creation event for activity schedule ${schedule.activityScheduleId}",
        it,
      )
    }
  }
}
