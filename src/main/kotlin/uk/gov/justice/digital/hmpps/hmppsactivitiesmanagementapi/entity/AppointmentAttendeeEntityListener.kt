package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Component
class AppointmentAttendeeEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Appointment instances are provided by a view that joins attendees, appointments and appointment series to create
   * a per prisoner per date individual appointment instance. This resulting appointment instance uses the attendee id
   * as there is a one-to-one relationship between attendees and instances. This is therefore the id that should be
   * used for the /appointment-instances/:id endpoint. Appointment instances are compatible with NOMIS appointments
   * stored in the OFFENDER_IND_SCHEDULES table. This event and associated endpoint are used primarily for sync purposes.
   */
  @PostPersist
  fun onCreate(entity: AppointmentAttendee) {
    runCatching {
      if (entity.appointment.appointmentSeries.isMigrated) {
        log.info("Not sending appointment instance created event for appointment instance id ${entity.appointmentAttendeeId} as it is a migration.")
      } else {
        outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, entity.appointmentAttendeeId)
      }
    }.onFailure {
      log.error(
        "Failed to send appointment instance created event for appointment instance id ${entity.appointmentAttendeeId}",
        it,
      )
    }
  }

  /**
   * Appointment instances are provided by a view that joins attendees, appointments and appointment series to create
   * a per prisoner per date individual appointment instance. This resulting appointment instance uses the attendee id
   * as there is a one-to-one relationship between attendees and instances. This is therefore the id that should be
   * used for the /appointment-instances/:id endpoint. Appointment instances are compatible with NOMIS appointments
   * stored in the OFFENDER_IND_SCHEDULES table. This event and associated endpoint are used primarily for sync purposes.
   */
  @PostUpdate
  fun onUpdate(entity: AppointmentAttendee) {
    runCatching {
      outboundEventsService.send(
        when {
          entity.isRemoved() -> OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
          entity.isDeleted -> OutboundEvent.APPOINTMENT_INSTANCE_DELETED

          else -> { OutboundEvent.APPOINTMENT_INSTANCE_UPDATED }
        },
        entity.appointmentAttendeeId,
      )
    }.onFailure {
      log.error(
        "Failed to send appointment instance updated event for appointment instance id ${entity.appointmentAttendeeId}",
        it,
      )
    }
  }

  /**
   * Appointment instances are provided by a view that joins attendees, appointments and appointment series to create
   * a per prisoner per date individual appointment instance. This resulting appointment instance uses the attendee id
   * as there is a one-to-one relationship between attendees and instances. This is therefore the id that should be
   * used for the /appointment-instances/:id endpoint. Appointment instances are compatible with NOMIS appointments
   * stored in the OFFENDER_IND_SCHEDULES table. This event and associated endpoint are used primarily for sync purposes.
   */
  @PostRemove
  fun onDelete(entity: AppointmentAttendee) {
    runCatching {
      outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, entity.appointmentAttendeeId)
    }.onFailure {
      log.error(
        "Failed to send appointment instance deleted event for appointment instance id ${entity.appointmentAttendeeId}",
        it,
      )
    }
  }
}
