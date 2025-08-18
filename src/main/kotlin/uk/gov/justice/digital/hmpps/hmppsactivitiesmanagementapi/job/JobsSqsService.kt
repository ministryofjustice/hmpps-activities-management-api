package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.function.Supplier

const val JOBS_QUEUE_NAME = "activitiesmanagementjobs"

@Service
class JobsSqsService(
  private val queueService: HmppsQueueService,
  private val mapper: ObjectMapper,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val queue by lazy { queueService.findByQueueId(JOBS_QUEUE_NAME) ?: throw RuntimeException("Queue with name $JOBS_QUEUE_NAME doesn't exist") }
  private val sqsClient by lazy { queue.sqsClient }
  private val queueUrl by lazy { queue.queueUrl }

  fun sendJobEvent(message: JobEventMessage) {
    log.info("Sending job event message $message")

    try {
      sqsClient.sendMessage(buildMessageRequest(message))
    } catch (e: Throwable) {
      val errorMessage = "Failed to send job event message $message"
      log.error(errorMessage, e)
      throw PublishEventException(errorMessage, e)
    }
    log.info("Successfully sent job event message $message")
  }

  private fun buildMessageRequest(event: JobEventMessage) = SendMessageRequest.builder()
    .queueUrl(queueUrl)
    .messageBody(mapper.writeValueAsString(event))
    .build()
}

data class JobEventMessage(
  val jobId: Long,
  val eventType: JobType,
  val messageAttributes: JobEvent,
)

class PublishEventException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PublishEventException> {
  override fun get(): PublishEventException = PublishEventException(message, cause)
}
