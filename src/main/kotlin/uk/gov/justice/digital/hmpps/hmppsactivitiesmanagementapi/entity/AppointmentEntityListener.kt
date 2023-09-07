package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Component
class AppointmentEntityListener {

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
   * stored in the OFFENDER_IND_SCHEDULES table. The appointment instance events and associated endpoint are used
   * primarily for sync purposes.
   *
   * As an appointment has a child collection of attendees and attendees represent appointment instances,
   * an update to an appointment implicitly applies to each child attendee. As a result an appointment instance
   * sync event should be published.
   *
   * The event type is determined by the state of the appointment at the time. If an appointment is cancelled or deleted,
   * the event is those types. A change to an active appointment is therefore an update event.
   */
  @PostUpdate
  fun onUpdate(entity: Appointment) {
    entity.attendees().forEach { attendee ->
      runCatching {
        outboundEventsService.send(
          when {
            entity.isCancelled() -> OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
            entity.isDeleted -> OutboundEvent.APPOINTMENT_INSTANCE_DELETED

            else -> { OutboundEvent.APPOINTMENT_INSTANCE_UPDATED }
          },
          attendee.appointmentAttendeeId,
        )
      }.onFailure {
        log.error(
          "Failed to send appointment instance updated event for appointment instance id ${attendee.appointmentAttendeeId}",
          it,
        )
      }
    }
  }
}
