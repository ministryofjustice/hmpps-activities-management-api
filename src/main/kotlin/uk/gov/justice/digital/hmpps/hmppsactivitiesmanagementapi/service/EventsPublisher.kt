package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class EventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val mapper: ObjectMapper,
  @Value("\${feature.events.sns.enabled:false}")
  private val enabled: Boolean,
) {

  companion object {
    const val TOPIC_ID = "domainevents"

    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("SNS enabled = $enabled")
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  internal fun send(event: OutboundHMPPSDomainEvent) {
    if (enabled) {
      domainEventsTopic.snsClient.publish(
        PublishRequest.builder()
          .topicArn(domainEventsTopic.arn)
          .message(mapper.writeValueAsString(event))
          .messageAttributes(metaData(event))
          .build(),
      ).also { log.info("Published $event") }

      return
    }

    log.info("Ignoring publishing event $event")
  }

  private fun metaData(payload: OutboundHMPPSDomainEvent): Map<String, MessageAttributeValue> {
    val messageAttributes: MutableMap<String, MessageAttributeValue> = HashMap()
    messageAttributes["eventType"] = MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build()
    return messageAttributes
  }
}
