package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.concurrent.TimeUnit

class LocalStackTestHelper {
  companion object {
    fun clearQueues(sqsClient: SqsAsyncClient, queue: HmppsQueue) {
      Awaitility.setDefaultPollDelay(1, TimeUnit.MILLISECONDS)
      Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS)

      sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.queueUrl).build()).get()
      sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.dlqUrl).build()).get()

      await untilCallTo {
        sqsClient.countAllMessagesOnQueue(queue.queueUrl).get() + sqsClient.countAllMessagesOnQueue(queue.dlqUrl!!).get()
      } matches { it == 0 }

      Awaitility.setDefaultPollInterval(50, TimeUnit.MILLISECONDS)

      sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.queueUrl).build()).get()
    }
  }
}
