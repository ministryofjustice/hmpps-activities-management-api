package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_ENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.SCHEDULES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@Profile("!test && !local")
@Service
class JobsSqsListener(
  private val scheduledInstancesService: ManageScheduledInstancesService,
  private val manageAllocationsService: ManageAllocationsDueToEndService,
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("activitiesmanagementjobs", factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(rawMessage: String) {
    log.debug("Received raw job event message $rawMessage")

    val sqsMessage = mapper.readValue(rawMessage, SQSMessage::class.java)

    when (sqsMessage.eventType) {
      SCHEDULES -> {
        scheduledInstancesService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      DEALLOCATE_ENDING -> {
        manageAllocationsService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      else -> {
        log.warn("Unsupported job event: ${sqsMessage.eventType}")
        throw UnsupportedOperationException("Unsupported job event: ${sqsMessage.eventType}")
      }
    }
  }

  data class SQSMessage(val jobId: Long, val eventType: JobType, val messageAttributes: Map<String, Any?>)

  private fun toPrisonCode(sqsMessage: SQSMessage) = mapper.convertValue(sqsMessage.messageAttributes, PrisonCodeJobEvent::class.java).prisonCode
}

interface JobEvent

data class PrisonCodeJobEvent(val prisonCode: String) : JobEvent
