package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class HmppsAuditApiClient(
  private val hmppsQueueService: HmppsQueueService,
) {

  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }
  private val objectMapper = jacksonObjectMapper()

  fun createEvent(event: HmppsAuditEvent) {
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(objectMapper.writeValueAsString(event))
        .build(),
    )
  }
}
