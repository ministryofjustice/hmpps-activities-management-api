package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

// TODO this is temporary measure whilst we are in Alpha phase.
@Profile(value = ["default"])
@Component
class MessageListener(
  private val mapper: ObjectMapper,
  private val inboundMessageService: InboundMessageService
) {

  @JmsListener(destination = "activities", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun incoming(jsonMessage: String?) {
    val (message) = mapper.readValue(jsonMessage, HMPPSMessage::class.java)

    inboundMessageService.handleEvent(mapper.readValue(message, HMPPSDomainEvent::class.java))
  }
}

data class HMPPSEventType(private val Value: String, private val Type: String)

data class HMPPSMessageAttributes(val eventType: HMPPSEventType)

data class HMPPSMessage(
  val Message: String,
  val MessageAttributes: HMPPSMessageAttributes
)
