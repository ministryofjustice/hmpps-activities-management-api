package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InboundEventsService

@Profile("!test && !local")
@Service
class EventListener(
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
  private val feature: FeatureSwitches,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("activities", factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(rawMessage: String) {
    val sqsMessage: SQSMessage = mapper.readValue(rawMessage)

    when (sqsMessage.Type) {
      "Notification" -> {
        mapper.readValue<HMPPSDomainEvent>(sqsMessage.Message).let { domainEvent ->
          domainEvent.toInboundEventType()?.let { inboundEventType ->
            if (feature.isEnabled(inboundEventType)) {
              inboundEventsService.process(inboundEventType.toInboundEvent(mapper, sqsMessage.Message))
            } else {
              log.warn("Inbound event type $inboundEventType feature is currently disabled.")
            }
          } ?: log.info("Ignoring domain event ${domainEvent.eventType}")
        }
      }
    }
  }
}

data class HMPPSDomainEvent(
  val eventType: String,
) {
  fun toInboundEventType() = InboundEventType.values().firstOrNull { it.eventType == eventType }
}

@Suppress("PropertyName")
@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SQSMessage(val Type: String, val Message: String, val MessageId: String? = null)
