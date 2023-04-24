package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_CREATED

@Service
class OutboundEventsService(private val publisher: OutboundEventsPublisher, private val featureSwitches: FeatureSwitches) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun send(outboundEvent: OutboundEvent, identifier: Long) {
    if (featureSwitches.isEnabled(outboundEvent)) {
      when (outboundEvent) {
        ACTIVITY_SCHEDULE_CREATED -> publisher.send(outboundEvent.event(ScheduleCreatedInformation(identifier)))
        ACTIVITY_SCHEDULED_INSTANCE_AMENDED -> publisher.send(outboundEvent.event(ScheduledInstanceInformation(identifier)))
        PRISONER_ALLOCATED -> publisher.send(outboundEvent.event(PrisonerAllocatedInformation(identifier)))
        PRISONER_ALLOCATION_AMENDED -> publisher.send(outboundEvent.event(PrisonerAllocatedInformation(identifier)))
        PRISONER_ATTENDANCE_CREATED -> publisher.send(outboundEvent.event(PrisonerAttendanceInformation(identifier)))
        PRISONER_ATTENDANCE_AMENDED -> publisher.send(outboundEvent.event(PrisonerAttendanceInformation(identifier)))
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
