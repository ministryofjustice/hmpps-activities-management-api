package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DomainEntityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DomainEntityUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_EXPIRED

@Service
class OutboundEventsService(
  private val publisher: OutboundEventsPublisher,
  private val featureSwitches: FeatureSwitches,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun <T> transactionalEventListener(event: DomainEntityCreatedEvent<T>) {
    when (event.source) {
      is ActivitySchedule -> send(ACTIVITY_SCHEDULE_CREATED, (event.source as ActivitySchedule).activityScheduleId)
      is Allocation -> send(PRISONER_ALLOCATED, (event.source as Allocation).allocationId)
      is Attendance -> send(PRISONER_ATTENDANCE_CREATED, (event.source as Attendance).attendanceId)
      else -> log.info("Unhandled create event $event")
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun <T> transactionalEventListener(event: DomainEntityUpdatedEvent<T>) {
    when (event.source) {
      is ActivitySchedule -> send(ACTIVITY_SCHEDULE_UPDATED, (event.source as ActivitySchedule).activityScheduleId)
      is Allocation -> send(PRISONER_ALLOCATION_AMENDED, (event.source as Allocation).allocationId)
      is Attendance -> send(PRISONER_ATTENDANCE_AMENDED, (event.source as Attendance).attendanceId)
      is ScheduledInstance -> send(ACTIVITY_SCHEDULED_INSTANCE_AMENDED, (event.source as ScheduledInstance).scheduledInstanceId)
      else -> log.info("Unhandled update event $event")
    }
  }

  fun send(outboundEvent: OutboundEvent, identifier: Long) {
    log.info("Sending event $outboundEvent for id $identifier")

    if (featureSwitches.isEnabled(outboundEvent)) {
      when (outboundEvent) {
        ACTIVITY_SCHEDULE_CREATED -> publisher.send(outboundEvent.event(ScheduleCreatedInformation(identifier)))
        ACTIVITY_SCHEDULE_UPDATED -> publisher.send(outboundEvent.event(ScheduleCreatedInformation(identifier)))

        ACTIVITY_SCHEDULED_INSTANCE_AMENDED -> publisher.send(outboundEvent.event(ScheduledInstanceInformation(identifier)))

        PRISONER_ALLOCATED -> publisher.send(outboundEvent.event(PrisonerAllocatedInformation(identifier)))
        PRISONER_ALLOCATION_AMENDED -> publisher.send(outboundEvent.event(PrisonerAllocatedInformation(identifier)))
        PRISONER_ATTENDANCE_CREATED -> publisher.send(outboundEvent.event(PrisonerAttendanceInformation(identifier)))
        PRISONER_ATTENDANCE_AMENDED -> publisher.send(outboundEvent.event(PrisonerAttendanceInformation(identifier)))
        PRISONER_ATTENDANCE_EXPIRED -> publisher.send(outboundEvent.event(PrisonerAttendanceInformation(identifier)))
        APPOINTMENT_INSTANCE_CREATED -> publisher.send(outboundEvent.event(AppointmentInstanceInformation(identifier)))
        APPOINTMENT_INSTANCE_UPDATED -> publisher.send(outboundEvent.event(AppointmentInstanceInformation(identifier)))
        APPOINTMENT_INSTANCE_DELETED -> publisher.send(outboundEvent.event(AppointmentInstanceInformation(identifier)))
        APPOINTMENT_INSTANCE_CANCELLED -> publisher.send(outboundEvent.event(AppointmentInstanceInformation(identifier)))
      }
    } else {
      log.warn("Outbound event type $outboundEvent feature is currently disabled.")
    }
  }
}
