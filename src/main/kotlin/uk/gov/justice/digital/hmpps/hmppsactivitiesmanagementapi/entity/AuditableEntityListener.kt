package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import toActivityCreatedEvent
import toActivityUpdatedEvent
import toPrisonerAddedToWaitingListEvent
import toPrisonerDeallocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import java.time.LocalDate

@Component
class AuditableEntityListener {

  @Autowired
  private lateinit var auditing: AuditUtil

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(entity: Any) {
    when (entity) {
      is Activity -> audit(entity.toActivityCreatedEvent(), "Failed to audit activity creation event for activity id ${entity.activityId}")
      is WaitingList -> audit(entity.toPrisonerAddedToWaitingListEvent(), "Failed to audit prisoner added to waiting list id ${entity.waitingListId}")
    }
  }

  @PostUpdate
  fun onUpdate(entity: Any) {
    when (entity) {
      is Activity -> audit(entity.toActivityUpdatedEvent(), "Failed to audit activity update event for activity id ${entity.activityId}")
      is Allocation -> audit(entity)
    }
  }

  private fun audit(allocation: Allocation) {
    when {
      allocation.isDeallocatedToday() -> audit(allocation.toPrisonerDeallocatedEvent(), "Failed to audit prisoner deallocation event for allocation id ${allocation.allocationId}")
      else -> log.warn("Ignoring audit of allocation id ${allocation.allocationId}")
    }
  }

  private fun Allocation.isDeallocatedToday() = isEnded() &&
    endDate == LocalDate.now() &&
    deallocatedBy != null &&
    deallocatedTime != null &&
    deallocatedReason != null

  private fun audit(auditableEvent: AuditableEvent, failureMessage: String) {
    runCatching {
      auditing.logEvent(auditableEvent)
    }.onFailure {
      log.error(failureMessage, it)
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
