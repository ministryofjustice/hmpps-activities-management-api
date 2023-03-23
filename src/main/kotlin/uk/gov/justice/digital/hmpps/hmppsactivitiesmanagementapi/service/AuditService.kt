package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.api.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.HmppsAuditable

@Service
class AuditService(
  private val hmppsAuditApiClient: HmppsAuditApiClient,
  private val objectMapper: ObjectMapper,
  @Value("\${feature.audit.service.enabled:false}")
  private val featureEnabled: Boolean,
) {

  fun logEvent(event: AuditableEvent) {
    if (featureEnabled && event is HmppsAuditable) {
      hmppsAuditApiClient.createEvent(
        HmppsAuditEvent(
          what = event.type().name,
          details = objectMapper.writeValueAsString(event),
        ),
      )
    }
  }
}
