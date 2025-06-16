package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems.UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.UUID

class UpdatesFromExternalSystemsEventsIntegrationTest : LocalStackTestBase() {
  private val updateFromExternalSystemEventsQueue by lazy {
    hmppsQueueService.findByQueueId(UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME) ?: throw MissingQueueException("HmppsQueue $UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME not found")
  }
  internal val queueSqsClient by lazy { updateFromExternalSystemEventsQueue.sqsClient }
  internal val queueUrl by lazy { updateFromExternalSystemEventsQueue.queueUrl }
  internal val queueSqsDlqClient by lazy { updateFromExternalSystemEventsQueue.sqsDlqClient as SqsAsyncClient }
  internal val dlqUrl by lazy { updateFromExternalSystemEventsQueue.dlqUrl as String }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int = queueSqsClient.countAllMessagesOnQueue(queueUrl).get()
  fun getNumberOfMessagesCurrentlyOnDlq(): Int = queueSqsDlqClient.countAllMessagesOnQueue(dlqUrl).get()

  @BeforeEach
  fun setup() {
    queueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build())
    queueSqsDlqClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(dlqUrl).build())
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
  }

  @Test
  fun `will handle a test event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "TestEvent",
      "description" : null,
      "messageAttributes" : {}
    }
    """

    queueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
  }

  @Test
  fun `will write an invalid event to the dlq`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "InvalidEventType",
      "description" : null,
      "messageAttributes" : {}
    }
    """

    queueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }
  }
}
