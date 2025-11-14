package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ALLOCATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_CREATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ATTENDANCE_EXPIRE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_ENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_EXPIRING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.END_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.MANAGE_APPOINTMENT_ATTENDEES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.SCHEDULES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.START_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ExpireAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToExpireService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAppointmentAttendeesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SuspendAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.UnsuspendAllocationsService
import java.time.LocalDate

@Profile("!test")
@Service
class JobsSqsListener(
  private val scheduledInstancesService: ManageScheduledInstancesService,
  private val manageAllocationsDueToEndService: ManageAllocationsDueToEndService,
  private val manageAllocationsDueToExpireService: ManageAllocationsDueToExpireService,
  private val manageNewAllocationsService: ManageNewAllocationsService,
  private val suspendAllocationsService: SuspendAllocationsService,
  private val unsuspendAllocationsService: UnsuspendAllocationsService,
  private val manageNewAttendancesService: ManageNewAttendancesService,
  private val expireAttendancesService: ExpireAttendancesService,
  private val manageAppointmentAttendeesService: ManageAppointmentAttendeesService,
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("activitiesmanagementjobs", factory = "hmppsQueueContainerFactoryProxy", maxMessagesPerPoll = "6", maxConcurrentMessages = "6")
  internal fun onMessage(rawMessage: String) {
    log.debug("Received raw job event message $rawMessage")

    val sqsMessage = mapper.readValue(rawMessage, SQSMessage::class.java)

    when (sqsMessage.eventType) {
      SCHEDULES -> {
        scheduledInstancesService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      DEALLOCATE_ENDING -> {
        manageAllocationsDueToEndService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      DEALLOCATE_EXPIRING -> {
        manageAllocationsDueToExpireService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      ALLOCATE -> {
        manageNewAllocationsService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      START_SUSPENSIONS -> {
        suspendAllocationsService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      END_SUSPENSIONS -> {
        unsuspendAllocationsService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      ATTENDANCE_CREATE -> {
        with(toNewActivityAttendance(sqsMessage)) {
          manageNewAttendancesService.handleEvent(sqsMessage.jobId, prisonCode, date, expireUnmarkedAttendances)
        }
      }

      ATTENDANCE_EXPIRE -> {
        expireAttendancesService.handleEvent(sqsMessage.jobId, toPrisonCode(sqsMessage))
      }

      MANAGE_APPOINTMENT_ATTENDEES -> {
        with(toManageAppointmentAttendeesJobEvent(sqsMessage)) {
          manageAppointmentAttendeesService.handleEvent(sqsMessage.jobId, prisonCode, daysAfterNow)
        }
      }

      else -> {
        log.warn("Unsupported job event: ${sqsMessage.eventType}")
        throw UnsupportedOperationException("Unsupported job event: ${sqsMessage.eventType}")
      }
    }
  }

  data class SQSMessage(val jobId: Long, val eventType: JobType, val messageAttributes: Map<String, Any?>)

  private fun toPrisonCode(sqsMessage: SQSMessage) = mapper.convertValue(sqsMessage.messageAttributes, PrisonCodeJobEvent::class.java).prisonCode

  private fun toNewActivityAttendance(sqsMessage: SQSMessage) = mapper.convertValue(sqsMessage.messageAttributes, NewActivityAttendanceJobEvent::class.java)

  private fun toManageAppointmentAttendeesJobEvent(sqsMessage: SQSMessage) = mapper.convertValue(sqsMessage.messageAttributes, ManageAppointmentAttendeesJobEvent::class.java)
}

interface JobEvent

data class PrisonCodeJobEvent(val prisonCode: String) : JobEvent

data class NewActivityAttendanceJobEvent(
  val prisonCode: String,
  val date: LocalDate,
  val expireUnmarkedAttendances: Boolean,
) : JobEvent

data class ManageAppointmentAttendeesJobEvent(
  val prisonCode: String,
  val daysAfterNow: Long,
) : JobEvent
