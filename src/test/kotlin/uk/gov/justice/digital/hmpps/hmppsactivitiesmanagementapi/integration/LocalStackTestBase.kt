package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.concurrent.TimeUnit

@ActiveProfiles("test-localstack", inheritProfiles = false)
abstract class LocalStackTestBase : ActivitiesIntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  protected val activitiesQueue by lazy { hmppsQueueService.findByQueueId("activities") as HmppsQueue }
  protected val activitiesQueueUrl by lazy { activitiesQueue.queueUrl }
  protected val activitiesClient by lazy { activitiesQueue.sqsClient }

  companion object {
    internal val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  @BeforeEach
  fun `clear queues`() {
    clearQueues(activitiesClient, activitiesQueue)
  }

  protected fun clearQueues(sqsClient: SqsAsyncClient, queue: HmppsQueue) {
    Awaitility.setDefaultPollDelay(1, TimeUnit.MILLISECONDS)
    Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS)

    sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.queueUrl).build()).get()

    await untilCallTo { sqsClient.countAllMessagesOnQueue(queue.queueUrl).get() } matches { it == 0 }

    Awaitility.setDefaultPollInterval(50, TimeUnit.MILLISECONDS)

    sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.queueUrl).build()).get()
  }

  protected fun sendInboundEvent(event: InboundEvent) {
    val sqsMessage = SQSMessage("Notification", mapper.writeValueAsString(event))

    activitiesQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(activitiesQueueUrl)
        .messageBody(mapper.writeValueAsString(sqsMessage))
        .build(),
    )
  }
}
