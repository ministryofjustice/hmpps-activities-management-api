package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This listener is responsible for recording creation of domain entities and updates to existing domain entities. When
 * the save/save all is called on the relevant entity repository an event will be raised to be intercepted by the
 * relevant event listener(s) in the service
 *
 * It is important to remember to call the save/save all on the repositories otherwise events will not be raised.
 */
class DomainEntityListener {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(value: Any) {
    if (value is DomainEventEntity<*>) value.registerCreate().also { log.info("Registering create event") }
  }

  @PostUpdate
  fun onUpdate(value: Any) {
    if (value is DomainEventEntity<*>) value.registerUpdate().also { log.info("Registering update event") }
  }
}
