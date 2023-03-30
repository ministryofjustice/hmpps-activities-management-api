package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.HmppsAuditable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.LocalAuditable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AuditRecordSearchFilters
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityUtils
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime

@Service
class AuditService(
  private val hmppsAuditApiClient: HmppsAuditApiClient,
  private val auditRepository: AuditRepository,
  private val objectMapper: ObjectMapper,
  @Value("\${feature.audit.service.enabled:false}")
  private val featureEnabled: Boolean,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Audit enabled = $featureEnabled")
  }

  fun searchEvents(
    page: Int,
    size: Int,
    sortDirection: String,
    filters: AuditRecordSearchFilters,
  ): Page<LocalAuditRecord> {
    val sort: Sort? = createSort(sortDirection, "recordedTime")
    val pageable: Pageable = if (sort != null) PageRequest.of(page, size, sort) else PageRequest.of(page, size)

    val results = auditRepository.searchRecords(
      prisonCode = filters.prisonCode,
      prisonerNumber = filters.prisonerNumber,
      username = filters.username,
      auditType = filters.auditType,
      auditEventType = filters.auditEventType,
      startTime = filters.startTime,
      endTime = filters.endTime,
      activityId = filters.activityId,
      scheduleId = filters.scheduleId,
      pageable = pageable,
    )

    return PageImpl(
      results.map { it.toModel() }.toList(),
      pageable,
      results.totalElements,
    )
  }

  fun logEvent(event: AuditableEvent) {
    if (featureEnabled) {
      if (event is HmppsAuditable) {
        hmppsAuditApiClient.createEvent(
          HmppsAuditEvent(
            what = event.auditEventType.name,
            details = objectMapper.writeValueAsString(event),
          ),
        )
      }

      if (event is LocalAuditable) {
        auditRepository.save(event.toLocalAuditRecord())
      }
    } else {
      log.info("Not auditing event of type ${event.javaClass.simpleName} as the feature is disabled")
    }
  }

  private fun createSort(sortDirection: String, sortField: String): Sort? {
    return when (sortDirection) {
      "ascending" -> Sort.by(sortField).ascending()
      "descending" -> Sort.by(sortField).descending()
      else -> null
    }
  }
}

@Component
class HmppsAuditApiClient(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  fun createEvent(event: HmppsAuditEvent) {
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(objectMapper.writeValueAsString(event))
        .build(),
    )
  }
}

data class HmppsAuditEvent(
  val what: String,
  val details: String,
) {
  val who = SecurityUtils.getUserNameForLoggedInUser()
  val `when`: LocalDateTime = LocalDateTime.now()
  val service = "hmpps-activities-management-api"
}
