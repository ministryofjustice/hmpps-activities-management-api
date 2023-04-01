package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toActivityCreatedEvent

@Component
class ActivityEntityListener {

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(activity: Activity) {
    // TODO Is there a way to make this work??
    val auditService = applicationContext.getBean(AuditService::class.java)

    runCatching {
      auditService.logEvent(activity.toActivityCreatedEvent())
    }.onFailure {
      log.error(
        "Failed to audit activity creation event for activity id ${activity.activityId}",
        it,
      )
    }
  }
}
