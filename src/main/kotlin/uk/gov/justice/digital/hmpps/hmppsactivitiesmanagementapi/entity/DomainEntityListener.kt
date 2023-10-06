package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
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
