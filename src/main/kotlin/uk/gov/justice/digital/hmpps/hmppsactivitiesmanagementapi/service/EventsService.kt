package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.ACTIVITY_SCHEDULE_CREATED
import java.time.LocalDateTime

@Service
class InboundEventsService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun receive(event: InboundHMPPSDomainEvent) {
    log.info("Ignoring received event: $event")
  }
}

@Service
class OutboundEventsService(private val publisher: EventsPublisher) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun send(outboundEvent: OutboundEvent, identifier: Long) {
    when (outboundEvent) {
      ACTIVITY_SCHEDULE_CREATED -> publisher.send(ACTIVITY_SCHEDULE_CREATED.event(identifier))
    }
  }
}

enum class OutboundEvent(val event: (identifier: Long) -> OutboundHMPPSDomainEvent) {
  ACTIVITY_SCHEDULE_CREATED(event = { identifier: Long ->
    OutboundHMPPSDomainEvent(
      eventType = "activities.activity-schedule.created",
      identifier = identifier,
      description = "new activity schedule with identifier $identifier has been created in the activities management service"
    )
  })
}

// TODO resolve message formats
data class OutboundHMPPSDomainEvent(
  val eventType: String,
  val identifier: Long,
  val version: String = "1",
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class InboundHMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation,
  val version: String,
  val occurredAt: String,
  val description: String
)

data class AdditionalInformation(
  val id: Long,
  val nomsNumber: String? = null,
  val reason: String? = null,
)
