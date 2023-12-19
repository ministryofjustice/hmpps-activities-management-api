package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Component
class ActivityScheduleEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostUpdate
  fun onUpdate(schedule: ActivitySchedule) {
    runCatching {
      outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, schedule.activityScheduleId)
    }.onFailure {
      log.error(
        "Failed to send activity schedule updated event for activity schedule id ${schedule.activityScheduleId}",
        it,
      )
    }
  }
}
