package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class EventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val mapper: ObjectMapper
) {

  companion object {
    const val TOPIC_ID = "domainevents"

    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  internal fun send(event: OutboundHMPPSDomainEvent) {
    // TODO commented out for now so as not to raise alerts, as nothing consume it.
    log.info("Ignoring publishing event $event")
//    domainEventsTopic.snsClient.publish(
//      PublishRequest(domainEventsTopic.arn, mapper.writeValueAsString(event))
//        .withMessageAttributes(
//          mapOf(
//            "eventType" to MessageAttributeValue().withDataType("String").withStringValue(event.eventType)
//          )
//        )
//    ).also { log.info("Published $event") }
  }
}
