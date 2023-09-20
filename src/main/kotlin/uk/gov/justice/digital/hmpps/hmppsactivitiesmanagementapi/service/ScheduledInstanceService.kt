package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate

@Service
class ScheduledInstanceService(
  private val repository: ScheduledInstanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val attendanceSummaryRepository: ScheduledInstanceAttendanceSummaryRepository,
  private var outboundEventsService: OutboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getActivityScheduleInstanceById(id: Long): ActivityScheduleInstance {
    val activityScheduleInstance = repository.findById(id)
      .orElseThrow { EntityNotFoundException("Scheduled Instance $id not found") }
    checkCaseloadAccess(activityScheduleInstance.activitySchedule.activity.prisonCode)
    return activityScheduleInstance.toModel()
  }

  @Transactional(readOnly = true)
  fun getActivityScheduleInstancesByDateRange(
    prisonCode: String,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<ActivityScheduleInstance> {
    val activities = repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
      prisonCode,
      dateRange.start,
      dateRange.endInclusive,
    ).toModel()

    return if (slot != null) {
      activities.filter { TimeSlot.slot(it.startTime) == slot }
    } else {
      activities
    }
  }

  fun uncancelScheduledInstance(id: Long) {
    log.info("Uncancelling scheduled instance $id")

    val scheduledInstance = repository.findById(id)
      .orElseThrow { EntityNotFoundException("Scheduled Instance $id not found") }

    scheduledInstance.uncancelSessionAndAttendances()

    val uncancelledInstance = repository.saveAndFlush(scheduledInstance)

    // Emit a sync event - manually
    if (!uncancelledInstance.cancelled) {
      send(uncancelledInstance.scheduledInstanceId)
    }

    log.info("Uncancelled scheduled instance $id")
  }

  fun cancelScheduledInstance(instanceId: Long, scheduleInstanceCancelRequest: ScheduleInstanceCancelRequest) {
    log.info("Cancelling scheduled instance $instanceId")

    val scheduledInstance = repository.findById(instanceId)
      .orElseThrow { EntityNotFoundException("Scheduled Instance $instanceId not found") }

    scheduledInstance.cancelSessionAndAttendances(
      reason = scheduleInstanceCancelRequest.reason,
      by = scheduleInstanceCancelRequest.username,
      cancelComment = scheduleInstanceCancelRequest.comment,
      cancellationReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
    )

    val cancelledInstance = repository.saveAndFlush(scheduledInstance)

    // Emit a sync event - manually
    if (cancelledInstance.cancelled) {
      send(cancelledInstance.scheduledInstanceId)
    }

    log.info("Cancelled scheduled instance $instanceId")
  }

  fun attendanceSummary(prisonCode: String, sessionDate: LocalDate): List<ScheduledInstanceAttendanceSummary> {
    checkCaseloadAccess(prisonCode)
    return attendanceSummaryRepository.findByPrisonAndDate(prisonCode, sessionDate).map { it.toModel() }
  }

  private fun send(instanceId: Long) {
    runCatching {
      outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instanceId)
    }.onFailure {
      log.error("Failed to send scheduled instance amended event for ID $instanceId", it)
    }
  }
}
