package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.HmppsAuditable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityUtils
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime

@Service
class AuditService(
  private val hmppsAuditApiClient: HmppsAuditApiClient,
  private val objectMapper: ObjectMapper,
  @Value("\${feature.audit.service.enabled:false}")
  private val featureEnabled: Boolean,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Audit enabled = $featureEnabled")
  }

  fun logEvent(event: AuditableEvent) {
    if (featureEnabled) {
      if (event is HmppsAuditable) {
        hmppsAuditApiClient.createEvent(
          HmppsAuditEvent(
            what = event.auditEventType.name,
            details = objectMapper.writeValueAsString(event),
          ),
        )
      }
    } else {
      log.info("Not auditing event of type ${event.javaClass.simpleName} as the feature is disabled")
    }
  }
}

@Component
class HmppsAuditApiClient(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  fun createEvent(event: HmppsAuditEvent) {
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(objectMapper.writeValueAsString(event))
        .build(),
    )
  }
}

data class HmppsAuditEvent(
  val what: String,
  val details: String,
) {
  val who = SecurityUtils.getUserNameForLoggedInUser()
  val `when`: LocalDateTime = LocalDateTime.now()
  val service = "hmpps-activities-management-api"
}
