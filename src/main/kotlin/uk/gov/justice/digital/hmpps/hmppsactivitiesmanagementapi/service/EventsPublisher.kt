package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventsPublisher {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  // TODO wire in the HmppsQueueService and send the message ...
  internal fun send(event: OutboundHMPPSDomainEvent) {
    log.info("Ignoring sending event: $event")
  }
}
