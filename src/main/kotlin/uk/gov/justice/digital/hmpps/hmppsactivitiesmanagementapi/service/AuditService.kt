package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.api.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.HmppsAuditable

@Service
class AuditService(
  private val hmppsAuditApiClient: HmppsAuditApiClient,
) {

  fun logEvent(event: AuditableEvent) {
    if (event is HmppsAuditable) {
      hmppsAuditApiClient.createEvent(event.toAuditEvent())
    }
  }
}
