package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import java.time.LocalDateTime

enum class OutboundEvent(val eventType: String) {
  ACTIVITY_SCHEDULE_CREATED("activities.activity-schedule.created") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A new activity schedule has been created in the activities management service",
      )
  },
  ACTIVITY_SCHEDULE_UPDATED("activities.activity-schedule.amended") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "An activity schedule has been updated in the activities management service",
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
  PRISONER_ATTENDANCE_DELETED("activities.prisoner.attendance-deleted") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A prisoner attendance has been deleted in the activities management service",
      )
  },
  PRISONER_ATTENDANCE_EXPIRED("activities.prisoner.attendance-expired") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "An unmarked prisoner attendance has been expired in the activities management service",
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
  APPOINTMENT_INSTANCE_CANCELLED("appointments.appointment-instance.cancelled") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "An appointment instance has been cancelled in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_UNCANCELLED("appointments.appointment-instance.uncancelled") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "An appointment instance has been uncancelled in the activities management service",
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
