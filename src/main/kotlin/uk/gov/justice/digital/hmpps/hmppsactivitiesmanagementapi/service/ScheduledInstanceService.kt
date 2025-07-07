package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toScheduledAttendeeModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstancesCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstancesUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduledInstancedUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.excludeTodayWithoutAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.toTelemetryPropertiesMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate

@Service
class ScheduledInstanceService(
  private val repository: ScheduledInstanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val attendanceSummaryRepository: ScheduledInstanceAttendanceSummaryRepository,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val outboundEventsService: OutboundEventsService,
  private val transactionHandler: TransactionHandler,
  private val telemetryClient: TelemetryClient,
  featureSwitches: FeatureSwitches,
) {
  private val useNewPriorityRules = featureSwitches.isEnabled(Feature.CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED)

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getActivityScheduleInstanceById(id: Long) = getScheduleInstanceById(id).toModel()

  private fun getScheduleInstanceById(id: Long): ScheduledInstance = repository.findById(id)
    .orElseThrow { EntityNotFoundException("Scheduled Instance $id not found") }
    .also {
      checkCaseloadAccess(it.activitySchedule.activity.prisonCode)
    }

  @Transactional(readOnly = true)
  fun getActivityScheduleInstancesByIds(ids: List<Long>) = repository.findByIds(ids)
    .also {
      it.forEach { instance -> checkCaseloadAccess(instance.activitySchedule.activity.prisonCode) }
    }
    .toModel(includeAllocations = false)

  @Transactional(readOnly = true)
  fun getActivityScheduleInstancesByDateRange(
    prisonCode: String,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
    cancelled: Boolean?,
  ): List<ActivityScheduleInstance> = repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
    prisonCode = prisonCode,
    startDate = dateRange.start,
    endDate = dateRange.endInclusive,
    cancelled = cancelled,
    timeSlot = slot,
  ).toModel()

  @Transactional(readOnly = true)
  fun getActivityScheduleInstancesForPrisonerByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate,
    slot: TimeSlot?,
  ): List<PrisonerScheduledActivity> {
    if (endDate.isAfter(startDate.plusMonths(3))) {
      throw ValidationException("Date range cannot exceed 3 months")
    }

    val filteredScheduledInstances = prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      startDate = startDate,
      endDate = endDate,
      timeSlot = slot,
    )

    return filteredScheduledInstances
  }

  fun getAttendeesForScheduledInstance(id: Long): List<ScheduledAttendee> {
    val activityScheduleInstance = repository.findOrThrowNotFound(id)
    checkCaseloadAccess(activityScheduleInstance.activitySchedule.activity.prisonCode)
    return prisonerScheduledActivityRepository.getAllByScheduledInstanceId(id)
      .excludeTodayWithoutAttendance()
      .toScheduledAttendeeModel()
  }

  fun uncancelScheduledInstance(id: Long) {
    log.info("Uncancelling scheduled instance $id")

    val (uncancelledInstance, uncancelledAttendances) = transactionHandler.newSpringTransaction {
      val scheduledInstance = repository.findById(id)
        .orElseThrow { EntityNotFoundException("Scheduled Instance $id not found") }

      val uncancelledAttendances = scheduledInstance.uncancelSessionAndAttendances(useNewPriorityRules)

      repository.saveAndFlush(scheduledInstance) to uncancelledAttendances
    }

    // Emit sync events - manually
    if (!uncancelledInstance.cancelled) {
      log.info("Sending instance amended and attendance amended events.")

      send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, uncancelledInstance.scheduledInstanceId)

      uncancelledAttendances.forEach { attendance ->
        send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
      }
    }

    log.info("Uncancelled scheduled instance $id")
  }

  fun uncancelScheduledInstances(request: ScheduleInstancesUncancelRequest) {
    log.info("Uncancelling ${request.scheduleInstanceIds!!.size} scheduled instances")

    transactionHandler.newSpringTransaction {
      repository.findByIds(request.scheduleInstanceIds.map { it })
        .map { scheduledInstance ->
          scheduledInstance to scheduledInstance.uncancelSessionAndAttendances(useNewPriorityRules)
        }
        .also {
          repository.saveAllAndFlush(it.map { it.first })
        }
    }
      .forEach { (uncancelledInstance, uncancelledAttendances) ->
        log.info("Sending instance amended and attendance amended events for uncancelled instance with id ${uncancelledInstance.scheduledInstanceId}")

        send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, uncancelledInstance.scheduledInstanceId)

        uncancelledAttendances.forEach { uncancelledAttendance ->
          send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, uncancelledAttendance.attendanceId)
        }
      }

    log.info("Finished uncancelling scheduled instances")
  }

  fun cancelScheduledInstance(instanceId: Long, scheduleInstanceCancelRequest: ScheduleInstanceCancelRequest) {
    log.info("Cancelling scheduled instance $instanceId")

    val (cancelledInstance, cancelledAttendances) = transactionHandler.newSpringTransaction {
      val scheduledInstance = repository.findById(instanceId)
        .orElseThrow { EntityNotFoundException("Scheduled Instance $instanceId not found") }

      val waitingAttendances = scheduledInstance.attendances.filter { it.status() == AttendanceStatus.WAITING }

      val cancelledAttendances = scheduledInstance.cancelSessionAndAttendances(
        reason = scheduleInstanceCancelRequest.reason,
        by = scheduleInstanceCancelRequest.username,
        cancelComment = scheduleInstanceCancelRequest.comment,
        cancellationReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED),
        useNewPriorityRules = useNewPriorityRules,
      )

      // If going from WAITING -> COMPLETED track as a RECORD_ATTENDANCE event
      waitingAttendances.forEach { attendance ->
        if (attendance.status() == AttendanceStatus.COMPLETED) {
          val propertiesMap = attendance.toTelemetryPropertiesMap()
          telemetryClient.trackEvent(TelemetryEvent.RECORD_ATTENDANCE.value, propertiesMap)
        }
      }

      repository.saveAndFlush(scheduledInstance) to cancelledAttendances
    }

    // Emit sync events - manually
    if (cancelledInstance.cancelled) {
      log.info("Sending instance amended and attendance amended events.")

      send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, cancelledInstance.scheduledInstanceId)

      cancelledAttendances.forEach { attendance ->
        send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
      }
    }

    log.info("Cancelled scheduled instance $instanceId")
  }

  fun cancelScheduledInstances(request: ScheduleInstancesCancelRequest) {
    log.info("Cancelling ${request.scheduleInstanceIds!!.size} scheduled instances")

    val cancellationReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)

    val attendancesPairsList = transactionHandler.newSpringTransaction {
      val scheduledInstances = repository.findByIds(request.scheduleInstanceIds.map { it })

      scheduledInstances.map { scheduledInstance ->
        val waitingAttendances = scheduledInstance.attendances.filter { it.status() == AttendanceStatus.WAITING }

        val cancelledAttendances = scheduledInstance.cancelSessionAndAttendances(
          reason = request.reason,
          by = request.username,
          cancelComment = request.comment,
          cancellationReason = cancellationReason,
          issuePayment = request.issuePayment!!,
          useNewPriorityRules = useNewPriorityRules,
        )

        // If going from WAITING -> COMPLETED track as a RECORD_ATTENDANCE event
        waitingAttendances.forEach { attendance ->
          if (attendance.status() == AttendanceStatus.COMPLETED) {
            val propertiesMap = attendance.toTelemetryPropertiesMap()
            telemetryClient.trackEvent(TelemetryEvent.RECORD_ATTENDANCE.value, propertiesMap)
          }
        }

        scheduledInstance to cancelledAttendances
      }
        .also { repository.saveAllAndFlush(scheduledInstances) }
    }

    attendancesPairsList
      .filter { it.first.cancelled }
      .forEach { (cancelledInstance, cancelledAttendances) ->
        // Emit sync events - manually
        log.info("Sending instance amended and attendance amended events for cancelled instance with id ${cancelledInstance.scheduledInstanceId}.")

        send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, cancelledInstance.scheduledInstanceId)

        cancelledAttendances.forEach { attendance ->
          send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
        }
      }
    log.info("Finished cancelling scheduled instances")
  }

  fun attendanceSummary(prisonCode: String, sessionDate: LocalDate): List<ScheduledInstanceAttendanceSummary> {
    checkCaseloadAccess(prisonCode)
    return attendanceSummaryRepository.findByPrisonAndDate(prisonCode, sessionDate).map { it.toModel() }
  }

  fun updateScheduledInstance(instanceId: Long, request: ScheduledInstancedUpdateRequest, updatedBy: String) {
    log.info("Updating scheduled instance $instanceId")

    val (instance, attendances) = transactionHandler.newSpringTransaction {
      var scheduledInstance = getScheduleInstanceById(instanceId)

      val attendances = scheduledInstance.updateCancelledSessionAndAttendances(
        reason = request.cancelledReason,
        updatedBy = updatedBy,
        cancelComment = request.comment,
        issuePayment = request.issuePayment,
      )

      repository.saveAndFlush(scheduledInstance) to attendances
    }

    // Emit sync events - manually
    if (instance.cancelled) {
      log.info("Sending instance amended and attendance amended events.")

      send(OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED, instance.scheduledInstanceId)

      attendances.forEach { attendance ->
        send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendance.attendanceId)
      }
    }

    log.info("Updated scheduled instance $instanceId")
  }

  private fun send(event: OutboundEvent, instanceId: Long) {
    runCatching {
      outboundEventsService.send(event, instanceId)
    }.onFailure {
      log.error("Failed to send scheduled instance amended event for ID $instanceId", it)
    }
  }
}
