package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.hmpps.sqs.HmppsQueueService

// TODO move to the events package
@Service
class EventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val mapper: ObjectMapper,
  features: FeatureSwitches,
) {
  private val outboundEventsEnabled = features.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)

  companion object {
    const val TOPIC_ID = "domainevents"

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Outbound SNS events enabled = $outboundEventsEnabled")
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  internal fun send(event: OutboundHMPPSDomainEvent) {
    if (outboundEventsEnabled) {
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

  private fun metaData(payload: OutboundHMPPSDomainEvent) =
    mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build())
}
