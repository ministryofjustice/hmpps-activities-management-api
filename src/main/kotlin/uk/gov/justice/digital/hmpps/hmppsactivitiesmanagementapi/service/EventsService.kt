package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.ACTIVITY_SCHEDULE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.APPOINTMENT_INSTANCE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent.PRISONER_ALLOCATED
import java.time.LocalDateTime

@Service
class InboundEventsService {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun receive(event: InboundHMPPSDomainEvent) {
    log.info("Ignoring received event: $event")
  }
}

@Service
class OutboundEventsService(private val publisher: EventsPublisher) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun send(outboundEvent: OutboundEvent, identifier: Long) {
    when (outboundEvent) {
      ACTIVITY_SCHEDULE_CREATED -> publisher.send(ACTIVITY_SCHEDULE_CREATED.event(ScheduleCreatedInformation(identifier)))
      PRISONER_ALLOCATED -> publisher.send(PRISONER_ALLOCATED.event(PrisonerAllocatedInformation(identifier)))
      APPOINTMENT_INSTANCE_CREATED -> publisher.send(APPOINTMENT_INSTANCE_CREATED.event(AppointmentInstanceCreatedInformation(identifier)))
      APPOINTMENT_INSTANCE_UPDATED -> publisher.send(APPOINTMENT_INSTANCE_UPDATED.event(AppointmentInstanceUpdatedInformation(identifier)))
      APPOINTMENT_INSTANCE_DELETED -> publisher.send(APPOINTMENT_INSTANCE_DELETED.event(AppointmentInstanceDeletedInformation(identifier)))
    }
  }
}

enum class OutboundEvent {
  ACTIVITY_SCHEDULE_CREATED {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = "activities.activity-schedule.created",
        additionalInformation = additionalInformation,
        description = "A new activity schedule has been created in the activities management service",
      )
  },
  PRISONER_ALLOCATED {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = "activities.prisoner.allocated",
        additionalInformation = additionalInformation,
        description = "A prisoner has been allocated to an activity in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_CREATED {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = "appointments.appointment-instance.created",
        additionalInformation = additionalInformation,
        description = "A new appointment instance has been created in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_UPDATED {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = "appointments.appointment-instance.updated",
        additionalInformation = additionalInformation,
        description = "An appointment instance has been updated in the activities management service",
      )
  },
  APPOINTMENT_INSTANCE_DELETED {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = "appointments.appointment-instance.deleted",
        additionalInformation = additionalInformation,
        description = "An appointment instance has been deleted in the activities management service",
      )
  }, ;

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

data class PrisonerAllocatedInformation(val allocationId: Long) : AdditionalInformation

data class AppointmentInstanceCreatedInformation(val appointmentInstanceId: Long) : AdditionalInformation

data class AppointmentInstanceUpdatedInformation(val appointmentInstanceId: Long) : AdditionalInformation

data class AppointmentInstanceDeletedInformation(val appointmentInstanceId: Long) : AdditionalInformation

// TODO format of inbound messages to be worked out when we start to consume them ...
data class InboundHMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: InboundAdditionalInformation,
  val version: String,
  val occurredAt: String,
  val description: String,
)

data class InboundAdditionalInformation(
  val id: Long,
  val nomsNumber: String? = null,
  val reason: String? = null,
) : AdditionalInformation
