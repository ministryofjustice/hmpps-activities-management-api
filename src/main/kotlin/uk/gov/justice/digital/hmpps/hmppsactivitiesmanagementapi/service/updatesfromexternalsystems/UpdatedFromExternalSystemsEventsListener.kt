package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

const val UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME = "updatefromexternalsystemevents"

@Profile("!test && !local")
@Component
class UpdatedFromExternalSystemsEventsListener(
  private val mapper: ObjectMapper
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Event listener started.")
  }

  @SqsListener(UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME, factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(rawMessage: String) {
    log.debug("Update from external system event raw message $rawMessage")

    val sqsMessage: SQSMessage = mapper.readValue(rawMessage)

    when (sqsMessage.Type) {
      "TestMessage" -> {}
      else -> {
        log.warn("Unrecognised message type on external system event: ${sqsMessage.Type}")
        throw Exception("Unrecognised message type on external system event: ${sqsMessage.Type}")
      }
    }
  }
}

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SQSMessage(val Type: String, val Message: String, val MessageId: String? = null)
