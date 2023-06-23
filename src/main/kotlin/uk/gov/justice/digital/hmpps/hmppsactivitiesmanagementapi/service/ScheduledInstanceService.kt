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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Service
@Transactional(readOnly = true)
class ScheduledInstanceService(
  private val repository: ScheduledInstanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private var outboundEventsService: OutboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getActivityScheduleInstanceById(id: Long): ActivityScheduleInstance {
    val activityScheduleInstance = repository.findById(id)
      .orElseThrow { EntityNotFoundException("Scheduled Instance $id not found") }
    return activityScheduleInstance.toModel()
  }

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

  @Transactional
  fun uncancelScheduledInstance(id: Long) {
    val scheduledInstance = repository.findById(id)
      .orElseThrow { EntityNotFoundException("Scheduled Instance $id not found") }

    scheduledInstance.uncancel()

    val uncancelledInstance = repository.saveAndFlush(scheduledInstance)

    // Emit a sync event - manually
    if (!uncancelledInstance.cancelled) {
      send(uncancelledInstance.scheduledInstanceId)
    }
  }

  @Transactional
  fun cancelScheduledInstance(instanceId: Long, scheduleInstanceCancelRequest: ScheduleInstanceCancelRequest) {
    val scheduledInstance = repository.findById(instanceId)
      .orElseThrow { EntityNotFoundException("Scheduled Instance $instanceId not found") }

    scheduledInstance.cancelSession(
      reason = scheduleInstanceCancelRequest.reason,
      by = scheduleInstanceCancelRequest.username,
      cancelComment = scheduleInstanceCancelRequest.comment,
    ) { attendanceList ->
      val attendanceReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)
      attendanceList.forEach {
        it.cancel(attendanceReason)
      }
    }

    val cancelledInstance = repository.saveAndFlush(scheduledInstance)

    // Emit a sync event - manually
    if (cancelledInstance.cancelled) {
      send(cancelledInstance.scheduledInstanceId)
    }
  }

  private fun send(instanceId: Long) {
    runCatching {
      outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instanceId)
    }.onFailure {
      log.error("Failed to send scheduled instance amended event for ID $instanceId", it)
    }
  }
}
