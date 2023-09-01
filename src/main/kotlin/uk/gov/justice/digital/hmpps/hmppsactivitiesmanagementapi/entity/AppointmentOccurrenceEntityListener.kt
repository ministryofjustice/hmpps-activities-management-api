package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Component
class AppointmentOccurrenceEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Appointment instances are provided by a view that joins allocations, occurrences and appointments to create
   * a per prisoner per date individual appointment instance. This resulting appointment instance uses the allocation id
   * as there is a one-to-one relationship between allocations and instances. This is therefore the id that should be
   * used for the /appointment-instances/:id endpoint. Appointment instances are compatible with NOMIS appointments
   * stored in the OFFENDER_IND_SCHEDULES table. The appointment instance events and associated endpoint are used
   * primarily for sync purposes.
   *
   * As an appointment occurrence has a child collection of allocations and allocations represent appointment instances,
   * an update to an appointment occurrence implicitly applies to each child allocation. As a result an appointment instance
   * sync event should be published.
   *
   * The event type is determined by the state of the occurrence at the time. If an occurrence is cancelled or deleted,
   * the event is those types. A change to an active appointment is therefore an update event.
   */
  @PostUpdate
  fun onUpdate(entity: AppointmentOccurrence) {
    entity.allocations().forEach { allocation ->
      runCatching {
        outboundEventsService.send(
          when {
            entity.isCancelled() -> OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
            entity.isDeleted() -> OutboundEvent.APPOINTMENT_INSTANCE_DELETED

            else -> { OutboundEvent.APPOINTMENT_INSTANCE_UPDATED }
          },
          allocation.appointmentOccurrenceAllocationId,
        )
      }.onFailure {
        log.error(
          "Failed to send appointment instance updated event for appointment instance id ${allocation.appointmentOccurrenceAllocationId}",
          it,
        )
      }
    }
  }
}
