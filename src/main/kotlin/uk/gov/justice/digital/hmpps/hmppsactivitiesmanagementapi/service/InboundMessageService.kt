package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InboundMessageService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleEvent(event: HMPPSDomainEvent) {
    log.info("Ignoring received event: $event")
  }
}

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation,
  val version: String,
  val occurredAt: String,
  val description: String
)

data class AdditionalInformation(
  val id: Long,
  val nomsNumber: String? = null,
  val reason: String? = null,
)
