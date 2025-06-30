package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.updatesfromexternalsystems.UpdateFromExternalSystemEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService

const val UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME = "updatefromexternalsystemevents"

@Profile("!test && !local")
@Component
class UpdateFromExternalSystemsEventsListener(
  private val mapper: ObjectMapper,
  private val attendancesService: AttendancesService,
  private val activityScheduleService: ActivityScheduleService,
) {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Updates from external systems event listener started.")
  }

  @SqsListener(UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME, factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(rawMessage: String) {
    log.debug("Update from external system event raw message $rawMessage")

    val sqsMessage = mapper.readValue(rawMessage, UpdateFromExternalSystemEvent::class.java)

    when (sqsMessage.eventType) {
      "TestEvent" -> {}
      "MarkPrisonerAttendance" -> {
        val event = sqsMessage.toMarkPrisonerAttendanceEvent()
        attendancesService.mark(principalName = sqsMessage.who, attendances = event.attendanceUpdateRequests)
      }
      "DeallocatePrisonerFromActivitySchedule" -> {
        val event = sqsMessage.toPrisonerDeallocationEvent()
        val prisonerDeallocationRequest = PrisonerDeallocationRequest(
          prisonerNumbers = event.prisonerNumbers,
          reasonCode = event.reasonCode,
          endDate = event.endDate,
          caseNote = event.caseNote,
          scheduleInstanceId = event.scheduleInstanceId,
        )
        val validationIssues = validator.validate(prisonerDeallocationRequest)
        if (validationIssues.isNotEmpty()) {
          throw ValidationException("Validation error on ${sqsMessage.eventType}: ${validationIssues.joinToString { it.message }}")
        }
        activityScheduleService.deallocatePrisoners(event.scheduleId, request = prisonerDeallocationRequest, deallocatedBy = sqsMessage.who)
      }
      "AllocatePrisonerToActivitySchedule" -> {
        val event = sqsMessage.toPrisonerAllocationEvent()
        val prisonerAllocationRequest = PrisonerAllocationRequest(
          prisonerNumber = event.prisonerNumber,
          payBandId = event.payBandId,
          startDate = event.startDate,
          endDate = event.endDate,
          exclusions = event.exclusions,
          scheduleInstanceId = event.scheduleInstanceId,
        )
        val validationIssues = validator.validate(prisonerAllocationRequest)
        if (validationIssues.isNotEmpty()) {
          throw ValidationException("Validation error on ${sqsMessage.eventType}: ${validationIssues.joinToString { it.message }}")
        }
        activityScheduleService.allocatePrisoner(event.scheduleId, request = prisonerAllocationRequest, allocatedBy = sqsMessage.who, adminMode = true)
      }
      else -> {
        log.warn("Unrecognised message type on external system event: ${sqsMessage.eventType}")
        throw Exception("Unrecognised message type on external system event: ${sqsMessage.eventType}")
      }
    }
  }
}
