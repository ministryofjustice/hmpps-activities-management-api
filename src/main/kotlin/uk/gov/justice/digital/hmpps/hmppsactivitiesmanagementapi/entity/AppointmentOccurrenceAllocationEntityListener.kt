package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEventsService

@Component
class AppointmentOccurrenceAllocationEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Appointment instances are provided by a view that joins allocations, occurrences and appointments to create
   * a per prisoner per date individual appointment instance. This resulting appointment instance uses the allocation id
   * as there is a one-to-one relationship between allocations and instances. Appointment instances are compatible with
   * NOMIS appointments stored in the OFFENDER_IND_SCHEDULES table. This event is used primarily for sync purposes.
   */
  @PostPersist
  fun onCreate(entity: AppointmentOccurrenceAllocation) {
    runCatching {
      outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, entity.appointmentOccurrenceAllocationId)
    }.onFailure {
      log.error(
        "Failed to send appointment instance creation event for activity schedule ${entity.appointmentOccurrenceAllocationId}",
        it,
      )
    }
  }
}
