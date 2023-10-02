package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches

@Profile("!test && !local")
@Component
class InboundEventsListener(
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
  private val feature: FeatureSwitches,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Event listener started.")
  }

  @SqsListener("activities", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "Digital-Prison-Services-hmpps_activities_management_queue", kind = SpanKind.SERVER)
  internal fun onMessage(rawMessage: String) {
    log.debug("Inbound event raw message $rawMessage")

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
      else -> log.warn("Unrecognised message type: ${sqsMessage.Type}")
    }
  }
}

data class HMPPSDomainEvent(
  val eventType: String,
) {
  fun toInboundEventType() = InboundEventType.values().firstOrNull { it.eventType == eventType }
}

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SQSMessage(val Type: String, val Message: String, val MessageId: String? = null)
