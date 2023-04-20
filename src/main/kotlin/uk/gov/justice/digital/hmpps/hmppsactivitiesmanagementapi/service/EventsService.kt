package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.ACTIVITY_SCHEDULE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.APPOINTMENT_INSTANCE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.PRISONER_ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.PRISONER_ATTENDANCE_CREATED
import java.time.LocalDateTime

// TODO move to events package and rename to match outbound service name
@Service
class OutboundEventsService(private val publisher: EventsPublisher, private val featureSwitches: FeatureSwitches) {
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
      }
    } else {
      log.warn("Outbound event type $outboundEvent feature is currently disabled.")
    }
  }
}

enum class OutboundEvent(val eventType: String) {
  ACTIVITY_SCHEDULE_CREATED("activities.activity-schedule.created") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A new activity schedule has been created in the activities management service",
      )
  },
  ACTIVITY_SCHEDULED_INSTANCE_AMENDED("activities.scheduled-instance.amended") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A scheduled instance has been amended in the activities management service",
      )
  },
  PRISONER_ALLOCATED("activities.prisoner.allocated") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A prisoner has been allocated to an activity in the activities management service",
      )
  },
  PRISONER_ALLOCATION_AMENDED("activities.prisoner.allocation-amended") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A prisoner allocation has been amended in the activities management service",
      )
  },
  PRISONER_ATTENDANCE_CREATED("activities.prisoner.attendance-created") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A prisoner attendance has been created in the activities management service",
      )
  },
  PRISONER_ATTENDANCE_AMENDED("activities.prisoner.attendance-amended") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A prisoner attendance has been amended in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_CREATED("appointments.appointment-instance.created") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A new appointment instance has been created in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_UPDATED("appointments.appointment-instance.updated") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "An appointment instance has been updated in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_DELETED("appointments.appointment-instance.deleted") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "An appointment instance has been deleted in the activities management service",
      )
  },
  ;

  abstract fun event(additionalInformation: AdditionalInformation): OutboundHMPPSDomainEvent
}

interface AdditionalInformation

data class OutboundHMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: String = "1",
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class ScheduleCreatedInformation(val activityScheduleId: Long) : AdditionalInformation
data class ScheduledInstanceInformation(val scheduledInstanceId: Long) : AdditionalInformation
data class PrisonerAllocatedInformation(val allocationId: Long) : AdditionalInformation
data class PrisonerAttendanceInformation(val attendanceId: Long) : AdditionalInformation
data class AppointmentInstanceInformation(val appointmentInstanceId: Long) : AdditionalInformation
