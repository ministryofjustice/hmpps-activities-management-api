package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toActivityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toPrisonerAllocatedEvent

@Component
class AuditableListener {

  @Autowired
  private lateinit var auditing: AuditUtil

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(entity: Any) {
    when (entity) {
      is Activity -> audit(entity)
      is Allocation -> audit(entity)
    }
  }

  private fun audit(activity: Activity) {
    runCatching {
      auditing.logEvent(activity.toActivityCreatedEvent())
    }.onFailure {
      log.error(
        "Failed to audit activity creation event for activity id ${activity.activityId}",
        it,
      )
    }
  }

  private fun audit(allocation: Allocation) {
    runCatching {
      auditing.logEvent(allocation.toPrisonerAllocatedEvent())
    }.onFailure {
      log.error(
        "Failed to audit prisoner allocation event for allocation id ${allocation.allocationId}",
        it,
      )
    }
  }
}

@Component
class AuditUtil {

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  fun logEvent(event: AuditableEvent) {
    applicationContext.getBean(AuditService::class.java).logEvent(event)
  }
}
