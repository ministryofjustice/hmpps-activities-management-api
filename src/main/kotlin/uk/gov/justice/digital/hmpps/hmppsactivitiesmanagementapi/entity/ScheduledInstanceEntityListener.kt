package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEventsService

@Component
class ScheduledInstanceEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostUpdate
  fun onUpdate(instance: ScheduledInstance) {
    send(
      OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED,
      instance.scheduledInstanceId,
      "Failed to send scheduled instance amended event for ID ${instance.scheduledInstanceId}",
    )
  }

  private fun send(outboundEvent: OutboundEvent, allocationId: Long, failureMessage: String) {
    runCatching {
      outboundEventsService.send(outboundEvent, allocationId)
    }.onFailure {
      log.error(failureMessage, it)
    }
  }
}
