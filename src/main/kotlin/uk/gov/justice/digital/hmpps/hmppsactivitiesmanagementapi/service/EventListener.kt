package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("!test && !local")
@Service
class EventListener(
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
) {
  @SqsListener("activities", factory = "hmppsQueueContainerFactoryProxy")
  fun incoming(jsonMessage: String?) {
    val (message) = mapper.readValue(jsonMessage, HMPPSMessage::class.java)

    inboundEventsService.receive(mapper.readValue(message, InboundHMPPSDomainEvent::class.java))
  }
}

data class HMPPSEventType(private val Value: String, private val Type: String)

data class HMPPSMessageAttributes(val eventType: HMPPSEventType)

data class HMPPSMessage(
  val Message: String,
  val MessageAttributes: HMPPSMessageAttributes,
)
