package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduleInstancesJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@Profile("!test && !local")
@Service
class JobsSqsListener(
  private val scheduledInstancesService: ManageScheduledInstancesService,
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("activitiesmanagementjobs", factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(rawMessage: String) {
    log.debug("Received raw job event message $rawMessage")

    val sqsMessage = mapper.readValue(rawMessage, SQSMessage::class.java)

    when (val event = sqsMessage.eventType.toJobEvent(mapper, sqsMessage.messageAttributes)) {
      is ScheduleInstancesJobEvent -> {
        scheduledInstancesService.handleCreateSchedulesEvent(sqsMessage.jobId, event.prisonCode)
      }

      else -> {
        log.warn("Unsupported job event: ${sqsMessage.eventType}")
        throw UnsupportedOperationException("Unsupported job event: ${sqsMessage.eventType}")
      }
    }
  }
}

data class SQSMessage(val jobId: Long, val eventType: JobType, val messageAttributes: Map<String, Any?>)
