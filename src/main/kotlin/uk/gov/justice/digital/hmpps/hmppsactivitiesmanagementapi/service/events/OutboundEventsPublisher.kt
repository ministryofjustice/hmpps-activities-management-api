package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sns.model.ValidationException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.OUTBOUND_DOMAIN_EVENT_DATE_TIME
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.OUTBOUND_DOMAIN_EVENT_DESCRIPTION
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.OUTBOUND_DOMAIN_EVENT_PRIMARY_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.OUTBOUND_DOMAIN_EVENT_SECONDARY_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.OUTBOUND_DOMAIN_EVENT_TYPE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.OUTBOUND_DOMAIN_EVENT_VERSION
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.publish
import java.util.concurrent.ExecutionException

@Service
class OutboundEventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val telemetryClient: TelemetryClient,
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
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw MissingQueueException("Topic with name $TOPIC_ID doesn't exist")
  }

  fun send(event: OutboundHMPPSDomainEvent) {
    if (!outboundEventsEnabled) {
      log.info("Ignoring publishing event $event")
      return
    }

    val publishResult: PublishResponse = try {
      domainEventsTopic.publish(
        eventType = event.eventType,
        event = mapper.writeValueAsString(event),
        attributes = metaData(event),
      )
        .also { log.debug("Published {}", event) }
    } catch (e: ExecutionException) {
      if (e.cause is ValidationException) {
        log.error("Invalid payload $event or other parameter", e.cause)
        telemetryClient.trackEvent("${event.eventType}_FAILED", asTelemetryMap(event), null)
        return
      } else {
        // Exception traceback will be logged by DefaultMessageListenerContainer
        log.error("Failed to publish message $event", e.cause)
        telemetryClient.trackEvent("${event.eventType}_FAILED", asTelemetryMap(event), null)
        throw e
      }
    } catch (e: Exception) {
      log.error("Failed to publish message $event", e)

      telemetryClient.trackEvent("${event.eventType}_FAILED", asTelemetryMap(event), null)
      throw e
    }
    val httpStatusCode = publishResult.sdkHttpResponse().statusCode()
    if (httpStatusCode >= 400) {
      telemetryClient.trackEvent("${event.eventType}_FAILED", asTelemetryMap(event), null)
      throw RuntimeException("Attempt to publish message ${event.eventType} resulted in an http $httpStatusCode error")
    }
    telemetryClient.trackEvent("${event.eventType}", asTelemetryMap(event), null)
  }

  private fun asTelemetryMap(event: OutboundHMPPSDomainEvent) = mapOf(
    OUTBOUND_DOMAIN_EVENT_TYPE to event.eventType,
    OUTBOUND_DOMAIN_EVENT_PRIMARY_ID to event.additionalInformation.primaryId(),
    OUTBOUND_DOMAIN_EVENT_SECONDARY_ID to event.additionalInformation.secondaryId(),
    OUTBOUND_DOMAIN_EVENT_VERSION to event.version,
    OUTBOUND_DOMAIN_EVENT_DESCRIPTION to event.description,
    OUTBOUND_DOMAIN_EVENT_DATE_TIME to event.occurredAt.toString(),
  )
    .filterValues { it != null }

  private fun metaData(payload: OutboundHMPPSDomainEvent) = mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build())
}
