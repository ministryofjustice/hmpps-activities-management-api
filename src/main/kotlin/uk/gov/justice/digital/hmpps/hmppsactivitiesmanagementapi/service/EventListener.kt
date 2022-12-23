package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service

// TODO this is temporary measure whilst we are in Alpha phase to reduce amount of code needed to run locally e.g. localstack.
@ConditionalOnProperty(name = ["JMS_LISTENER_MODE"], havingValue = "ON")
@Service
class EventListener(
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService
) {

  @JmsListener(destination = "activities", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun incoming(jsonMessage: String?) {
    val (message) = mapper.readValue(jsonMessage, HMPPSMessage::class.java)

    inboundEventsService.receive(mapper.readValue(message, InboundHMPPSDomainEvent::class.java))
  }
}

data class HMPPSEventType(private val Value: String, private val Type: String)

data class HMPPSMessageAttributes(val eventType: HMPPSEventType)

data class HMPPSMessage(
  val Message: String,
  val MessageAttributes: HMPPSMessageAttributes
)
