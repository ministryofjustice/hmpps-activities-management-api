package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.updatesfromexternalsystems.UpdateFromExternalSystemEvent

const val UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME = "updatefromexternalsystemevents"

@Profile("!test && !local")
@Component
class UpdateFromExternalSystemsEventsListener(
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Updates from external systems event listener started.")
  }

  @SqsListener(UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME, factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(rawMessage: String) {
    log.debug("Update from external system event raw message $rawMessage")

    val sqsMessage = mapper.readValue(rawMessage, UpdateFromExternalSystemEvent::class.java)

    when (sqsMessage.eventType) {
      "TestEvent" -> {}
      else -> {
        log.warn("Unrecognised message type on external system event: ${sqsMessage.eventType}")
        throw Exception("Unrecognised message type on external system event: ${sqsMessage.eventType}")
      }
    }
  }
}
