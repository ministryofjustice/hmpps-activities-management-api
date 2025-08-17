package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduleInstancesJobEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

class JobsSqsServiceTest {
  val queueService: HmppsQueueService = mock()
  val mapper = jacksonObjectMapper()

  val service = JobsSqsService(queueService, mapper)

  val sqsClient: SqsAsyncClient = mock()

  @BeforeEach
  fun setUp() {
    val queue: HmppsQueue = mock()

    whenever(queueService.findByQueueId(JOBS_QUEUE_NAME)).thenReturn(queue)
    whenever(queue.sqsClient).thenReturn(sqsClient)
    whenever(queue.queueUrl).thenReturn("queue url")
  }

  @Test
  fun `should send job event`() {
    val message = JobEventMessage(123, JobType.SCHEDULES, ScheduleInstancesJobEvent("RSI"))

    val expectedMessage = SendMessageRequest.builder()
      .queueUrl("queue url")
      .messageBody(mapper.writeValueAsString(message))
      .build()

    service.sendJobEvent(message)

    verify(sqsClient).sendMessage(expectedMessage)
  }

  @Test
  fun `should throw exception if sendMessage failed`() {
    val message = JobEventMessage(123, JobType.SCHEDULES, ScheduleInstancesJobEvent("RSI"))

    whenever(sqsClient.sendMessage(any<SendMessageRequest>())).thenThrow(RuntimeException("Failed to send message"))

    assertThrows<PublishEventException>("Failed to send job event message $message") {
      service.sendJobEvent(message)
    }
  }
}
