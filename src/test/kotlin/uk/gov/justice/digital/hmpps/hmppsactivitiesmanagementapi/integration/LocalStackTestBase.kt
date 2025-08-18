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

@ActiveProfiles("test-local-stack", inheritProfiles = false)
abstract class LocalStackTestBase : ActivitiesIntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  protected val activitiesQueue by lazy { hmppsQueueService.findByQueueId("activities") as HmppsQueue }
  protected val activitiesClient by lazy { activitiesQueue.sqsClient }

  protected val jobsQueue by lazy { hmppsQueueService.findByQueueId("activitiesmanagementjobs") as HmppsQueue }
  protected val jobsClient by lazy { jobsQueue.sqsClient }

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
    Awaitility.setDefaultPollDelay(1, TimeUnit.MILLISECONDS)
    Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS)

    activitiesClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(activitiesQueue.queueUrl).build()).get()

    jobsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(jobsQueue.queueUrl).build()).get()

    await untilCallTo {
      countMessagesOnActivitiesQueue() + countMessagesOnJobsQueue()
    } matches { it == 0 }

    Awaitility.setDefaultPollInterval(50, TimeUnit.MILLISECONDS)
  }

  protected fun countMessagesOnActivitiesQueue(): Int = activitiesClient.countAllMessagesOnQueue(activitiesQueue.queueUrl).get()

  protected fun countMessagesOnJobsQueue(): Int = jobsClient.countAllMessagesOnQueue(jobsQueue.queueUrl).get()

  protected fun sendInboundEvent(event: InboundEvent) {
    val sqsMessage = SQSMessage("Notification", mapper.writeValueAsString(event))

    activitiesQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl("activities")
        .messageBody(mapper.writeValueAsString(sqsMessage))
        .build(),
    )
  }
}
